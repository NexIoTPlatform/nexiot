#!/usr/bin/env python3
"""Exercise the isolated OSS stack via real APIs and MQTT. Never stores auth material.

Requires NEXIOT_ACCEPTANCE_ADMIN_PASSWORD in the environment. Target addresses are
fixed to the isolated local ports so this script cannot affect an existing stack.
"""
from __future__ import annotations
import argparse
import base64
from datetime import datetime, timezone
import json
import hashlib
import hmac
import os
from pathlib import Path
import secrets
import shlex
import socket
import struct
import subprocess
import tempfile
import time
import urllib.error
import urllib.parse
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
BASE = 'http://127.0.0.1:19092'


def runtime():
    result = {}
    path = ROOT / '.runtime/cn-universal-web/config/runtime.env'
    if path.exists():
        for line in path.read_text().splitlines():
            line = line.strip().removeprefix('export ')
            if line and not line.startswith('#') and '=' in line:
                key, value = line.split('=', 1)
                result[key] = ' '.join(shlex.split(value))
    return result


class Api:
    def __init__(self):
        self.token = None

    def call(self, method, path, body=None, form=None, headers=None, expected=(200, 201, 202)):
        request_headers = {'Accept': 'application/json', **(headers or {})}
        parsed_path = urllib.parse.urlsplit(path)
        timestamp = str(int(time.time()))
        nonce = secrets.token_hex(12)
        signature_payload = '\n'.join((method.upper(), parsed_path.path,
            urllib.parse.unquote_plus(parsed_path.query), timestamp, nonce))
        sign_secret = os.environ.get('NEXIOT_ACCEPTANCE_SIGN_SECRET', runtime().get('REQUEST_SIGN_SECRET', 'nexiot'))
        request_headers.update({'X-Sign-Timestamp': timestamp, 'X-Sign-Nonce': nonce,
            'X-Signature': hmac.new(sign_secret.encode(), signature_payload.encode(), hashlib.sha256).hexdigest().upper()})
        if self.token:
            request_headers['Authorization'] = 'Bearer ' + self.token
        data = None
        if form is not None:
            data = urllib.parse.urlencode(form).encode()
            request_headers['Content-Type'] = 'application/x-www-form-urlencoded'
        elif body is not None:
            data = json.dumps(body).encode()
            request_headers['Content-Type'] = 'application/json'
        request = urllib.request.Request(BASE + path, data=data, headers=request_headers, method=method)
        try:
            with urllib.request.urlopen(request, timeout=20) as response:
                status = response.status
                result = json.load(response)
        except urllib.error.HTTPError as error:
            if error.code in expected:
                return json.loads(error.read())
            raise RuntimeError(f'{method} {path.split("?")[0]} returned HTTP {error.code}') from None
        if status not in expected:
            raise RuntimeError(f'{method} {path.split("?")[0]} returned HTTP {status}')
        if isinstance(result, dict) and result.get('code', 200) not in (0, 200):
            message = str(result.get('msg', 'application request failed'))[:200]
            raise RuntimeError(f'{method} {path.split("?")[0]}: {message}')
        return result

    def login(self, username, password, tenant_code=None, captcha=False):
        form = {'username': username, 'password': password, 'grant_type': 'password',
                'client_id': 'web', 'client_secret': 'web'}
        if tenant_code:
            form['tenantCode'] = tenant_code
        if captcha:
            challenge = self.call('GET', '/getCaptchaCode')['data']
            key = challenge['key']
            code = redis_command('GET', 'captcha_codes:' + challenge['uuid'])
            if not code:
                raise RuntimeError('Isolated captcha was unavailable in Redis')
            pem = '-----BEGIN PUBLIC KEY-----\n' + '\n'.join(key[i:i+64] for i in range(0, len(key), 64)) + '\n-----END PUBLIC KEY-----\n'
            with tempfile.NamedTemporaryFile(mode='w') as public_key:
                public_key.write(pem)
                public_key.flush()
                encrypted = subprocess.run(['openssl', 'pkeyutl', '-encrypt', '-pubin', '-inkey', public_key.name,
                                            '-pkeyopt', 'rsa_padding_mode:pkcs1'], input=password.encode(),
                                           capture_output=True, check=True).stdout
            form.update({'grant_type': 'captcha', 'password': base64.b64encode(encrypted).decode(),
                         'code': code.decode(), 'uuid': challenge['uuid']})
        result = self.call('POST', '/oauth/token', form=form)
        self.token = result.get('access_token')
        if not self.token:
            raise RuntimeError('Login returned no access token')


def redis_command(*parts):
    settings = runtime()
    assert settings.get('REDIS_HOST', '127.0.0.1') == '127.0.0.1'
    assert int(settings.get('REDIS_PORT', '26379')) == 26379
    with socket.create_connection(('127.0.0.1', 26379), timeout=5) as client:
        file = client.makefile('rb')
        def command(*values):
            values = [str(value).encode() for value in values]
            client.sendall(b'*' + str(len(values)).encode() + b'\r\n' + b''.join(
                b'$' + str(len(value)).encode() + b'\r\n' + value + b'\r\n' for value in values))
            line = file.readline()
            if line.startswith(b'$'):
                length = int(line[1:])
                if length < 0:
                    return None
                value = file.read(length)
                assert file.read(2) == b'\r\n'
                return value
            if line.startswith(b'-'):
                raise RuntimeError('Isolated Redis operation failed')
            return line[1:-2]
        if settings.get('REDIS_PASSWORD'):
            command('AUTH', settings['REDIS_PASSWORD'])
        command('SELECT', settings.get('REDIS_DATABASE', '0'))
        return command(*parts)


def mqtt_publish(topic, payload):
    def string(value):
        value = value.encode()
        return struct.pack('!H', len(value)) + value
    def packet(kind, payload):
        length = len(payload)
        encoded = bytearray()
        while True:
            byte = length % 128
            length //= 128
            encoded.append(byte | (128 if length else 0))
            if not length:
                return bytes([kind]) + bytes(encoded) + payload
    with socket.create_connection(('127.0.0.1', 21883), timeout=10) as client:
        client.settimeout(10)
        connect = string('MQTT') + b'\x04\x02\x00\x3c' + string('oss-acceptance-' + secrets.token_hex(5))
        client.sendall(packet(0x10, connect))
        response = client.recv(4)
        if response != b'\x20\x02\x00\x00':
            raise RuntimeError('Isolated broker refused MQTT connection')
        client.sendall(packet(0x32, string(topic) + b'\x00\x01' + json.dumps(payload).encode()))
        ack = client.recv(4)
        if ack != b'\x40\x02\x00\x01':
            raise RuntimeError('MQTT broker did not acknowledge QoS1 publish')
        client.sendall(b'\xe0\x00')


def data(response):
    return response.get('data', response)


def rows(response):
    value = data(response)
    if isinstance(value, list):
        return value
    return value.get('rows', value.get('records', value.get('list', [])))


def flatten(menus):
    return [item for menu in menus for item in [menu, *flatten(menu.get('children', []))]]


def query(path, **parameters):
    return path + '?' + urllib.parse.urlencode(parameters)


def wait_for_value(api, device, expected):
    deadline = time.monotonic() + 40
    while time.monotonic() < deadline:
        result = api.call('GET', '/admin/v1/device/shadow/' + str(device['iotId']))
        if str(expected) in json.dumps(data(result)):
            return data(result)
        time.sleep(1)
    raise RuntimeError('Reported temperature did not reach the device shadow')


def sample_temperature(api, device, whole):
    shadow = data(api.call('GET', '/admin/v1/device/shadow/' + str(device['iotId']))) or {}
    try:
        previous = json.loads(shadow.get('metadata', '{}')).get('state', {}).get('reported', {}).get('temperature')
    except (TypeError, ValueError):
        previous = None
    # The fixture's double field uses the platform's default two-decimal precision.
    while True:
        value = whole + secrets.randbelow(100) / 100
        if value != previous:
            return value


def wait_for_history(api, product, device, expected):
    deadline = time.monotonic() + 30
    while time.monotonic() < deadline:
        response = api.call('GET', query('/admin/v2/device/log/list', iotId=device['iotId'],
            productKey=product['productKey'], pageNum=1, pageSize=10))
        if str(expected) in json.dumps(response):
            return response
        time.sleep(1)
    raise RuntimeError('Reported temperature did not reach the persisted history API')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--run-id', default=datetime.now().strftime('%m%d%H%M%S'))
    parser.add_argument('--report', type=Path, default=ROOT / 'validation/api-acceptance.json')
    args = parser.parse_args()
    password = os.environ.get('NEXIOT_ACCEPTANCE_ADMIN_PASSWORD')
    if not password:
        parser.error('NEXIOT_ACCEPTANCE_ADMIN_PASSWORD must be provided; it is never saved')
    report = {'started_at': datetime.now(timezone.utc).isoformat(), 'base_url': BASE,
              'run_id': args.run_id, 'checks': {}, 'test_data': {}, 'observations': {}, 'status': 'running'}
    report['test_data_policy'] = 'Synthetic tenant/products/devices retained only in the isolated local OSS database for UI review; credentials and tokens are omitted.'
    checks = report['checks']
    stage = 'health'
    try:
        admin = Api()
        assert admin.call('GET', '/actuator/health')['status'] == 'UP'
        checks['health'] = 'passed'
        stage = 'admin_login'
        admin.login('admin', password, captcha=True)
        info = data(admin.call('GET', '/admin/getInfo'))
        assert info['user']['username'] == 'admin' and info['user']['admin']
        checks['captcha_rsa_admin_login_and_get_info'] = 'passed'
        menus = flatten(data(admin.call('GET', '/admin/getRouters')))
        components = {item.get('component', '') for item in menus}
        assert not any(value.startswith(('video/', 'videoai/', 'scada/', 'flow/', 'iot/point-template/')) for value in components)
        checks['excluded_menu_components_absent'] = 'passed'
        report['admin_menu_count'] = len(menus)
        protocol_status = data(admin.call('GET', '/admin/protocol/status'))
        assert set(protocol_status['protocols']) == {'mqtt', 'http'}
        checks['only_mqtt_and_http_protocols_registered'] = 'passed'
        stage = 'tenant_create'
        tenant_code = 'ossacceptance-' + args.run_id
        username = 'ossapi' + args.run_id
        tenants = rows(admin.call('GET', query('/admin/system/tenant/list', tenantCode=tenant_code, pageNum=1, pageSize=100)))
        matching = [item for item in tenants if item.get('tenantCode') == tenant_code]
        if not matching:
            admin.call('POST', '/admin/system/tenant', {
                'tenantCode': tenant_code, 'tenantName': 'OSS API acceptance ' + args.run_id,
                'packageId': '900000000000000001', 'status': 0, 'accountCount': 3,
                'adminUsername': username, 'adminAlias': 'OSS API acceptance',
                'adminMobile': '199' + args.run_id[-8:].zfill(8), 'adminPassword': password,
                'remark': 'Disposable local OSS validation; contains synthetic data only'})
            tenants = rows(admin.call('GET', query('/admin/system/tenant/list', tenantCode=tenant_code, pageNum=1, pageSize=100)))
            matching = [item for item in tenants if item.get('tenantCode') == tenant_code]
        assert len(matching) == 1
        tenant_id = matching[0]['tenantId']
        report['test_data'].update({'tenant_id': tenant_id, 'tenant_code': tenant_code, 'username': username})
        tenant = Api()
        tenant.login(username, password, tenant_code=tenant_code, captcha=True)
        tenant_info = data(tenant.call('GET', '/admin/getInfo'))
        assert str(tenant_info['user']['tenantId']) == str(tenant_id) and not tenant_info['user']['admin']
        checks['tenant_api_create_captcha_login_and_scope'] = 'passed'
        stage = 'category_create'
        category_name = 'OSS acceptance ' + args.run_id
        category_items = data(tenant.call('GET', '/admin/sort/list'))
        category = next((item for item in category_items if item['classifiedName'] == category_name), None)
        if category is None:
            tenant.call('POST', '/admin/sort/add', {'parentId': '0', 'classifiedName': category_name,
                                                  'identification': 'oss' + args.run_id})
            category = next(item for item in data(tenant.call('GET', '/admin/sort/list')) if item['classifiedName'] == category_name)
        checks['tenant_product_category_create'] = 'passed'
        products = {}
        devices = {}
        for protocol in ('mqtt', 'http'):
            stage = protocol + '_product_device_create'
            name = 'OSS ' + protocol.upper() + ' ' + args.run_id
            product_items = rows(tenant.call('GET', query('/admin/v1/product/list', name=name, pageNum=1, pageSize=100)))
            product = next((item for item in product_items if item['name'] == name), None)
            if product is None:
                product = data(tenant.call('POST', '/admin/v1/product', {
                    'name': name, 'thirdPlatform': protocol, 'classifiedId': category['id'],
                    'classifiedName': category_name, 'companyNo': 'OSS', 'productId': protocol + args.run_id,
                    'deviceNode': 'DEVICE', 'state': 0, 'describe': 'Synthetic local acceptance product'}))
            products[protocol] = product
            detail = data(tenant.call('GET', '/admin/v1/product/' + str(product['id'])))
            if 'temperature' not in str(detail.get('metadata', '')):
                tenant.call('POST', '/admin/v1/product/metadata/add', {
                    'productKey': product['productKey'], 'type': 'properties',
                    'ioTProductMetadataBO': {'id': 'temperature', 'name': 'Temperature', 'mode': 'r',
                                           'valueType': {'type': 'double', 'unit': '°C'}, 'description': 'Synthetic validation reading'}})
            device_id = 'oss-' + protocol + '-' + args.run_id
            device_items = rows(tenant.call('GET', query('/admin/v1/device/list', productKey=product['productKey'], pageNum=1, pageSize=100)))
            device = next((item for item in device_items if item['deviceId'] == device_id), None)
            if device is None:
                tenant.call('POST', '/admin/v1/device', {'productKey': product['productKey'], 'deviceId': device_id,
                                                       'deviceName': 'OSS ' + protocol + ' device', 'detail': 'Synthetic local acceptance device'})
                device_items = rows(tenant.call('GET', query('/admin/v1/device/list', productKey=product['productKey'], pageNum=1, pageSize=100)))
                device = next(item for item in device_items if item['deviceId'] == device_id)
            devices[protocol] = device
            checks[protocol + '_product_device_metadata_create'] = 'passed'
            report['test_data'][protocol] = {'product_id': product['id'], 'product_key': product['productKey'],
                                            'device_id': device['deviceId'], 'iot_id': device['iotId']}
            if protocol == 'mqtt':
                stage = 'mqtt_report'
                mqtt_value = sample_temperature(tenant, devices['mqtt'], 23)
                report['observations']['mqtt_temperature'] = mqtt_value
                mqtt_publish('$thing/up/property/' + products['mqtt']['productKey'] + '/' + devices['mqtt']['deviceId'],
                             {'temperature': mqtt_value})
                wait_for_value(tenant, devices['mqtt'], mqtt_value)
                wait_for_history(tenant, products['mqtt'], devices['mqtt'], mqtt_value)
                checks['mqtt_qos1_publish_shadow_and_history_api_read'] = 'passed'
        stage = 'http_driver'
        source = """import cn.hutool.json.JSONUtil;
var preDecode = (payload, context) => { var value = JSONUtil.parseObj(payload); return {deviceId: value.deviceId, payload: payload}; };
var decode = (payload, context) => { var value = JSONUtil.parseObj(payload); return {messageType: 'PROPERTIES', properties: {temperature: value.temperature}}; };
var encode = (payload, context) => { return payload; };
"""
        driver = {'id': products['http']['productKey'], 'name': 'OSS HTTP acceptance driver', 'type': 'magic',
                  'state': 1, 'fileName': 'oss-http-' + args.run_id, 'jscript': source, 'description': 'Synthetic acceptance transform'}
        existing_driver = next((item for item in rows(tenant.call('GET', query('/admin/v1/protocol/list', pageNum=1, pageSize=100)))
                                if item['id'] == products['http']['productKey']), None)
        tenant.call('PUT' if existing_driver else 'POST', '/admin/v1/protocol', driver)
        preview = data(tenant.call('POST', '/admin/v1/protocol/codec', {
            'id': products['http']['productKey'], 'decode': True,
            'payload': json.dumps({'deviceId': devices['http']['deviceId'], 'temperature': 27.125})}))
        assert '27.125' in json.dumps(preview)
        checks['http_magic_driver_save_and_decode'] = 'passed'
        stage = 'http_network'
        access_code = 'oss-' + args.run_id
        api_key = secrets.token_urlsafe(24)
        config = {'schemaVersion': 2, 'revision': 1, 'accessCode': access_code,
                  'inbound': {'auth': {'type': 'API_KEY', 'headerName': 'X-API-Key', 'value': api_key}},
                  'outbound': {'allowPrivateNetwork': False}}
        assert tenant.call('POST', '/admin/http-access/networks/validate', {'configuration': config})['valid']
        networks = rows(tenant.call('GET', query('/admin/network/list', type='HTTP_SERVER', pageNum=1, pageSize=100)))
        network = next((item for item in networks if item['name'] == 'OSS HTTP ' + args.run_id), None)
        network_body = {'type': 'HTTP_SERVER', 'productKey': products['http']['productKey'],
                        'name': 'OSS HTTP ' + args.run_id, 'state': 1, 'configuration': json.dumps(config)}
        if network:
            config['revision'] = int(json.loads(network.get('configuration', '{}')).get('revision', 1)) + 1
            network_body.update({'id': network['id'], 'unionId': network['unionId'], 'configuration': json.dumps(config)})
        tenant.call('PUT' if network else 'POST', '/admin/network', network_body)
        network = next(item for item in rows(tenant.call('GET', query('/admin/network/list', type='HTTP_SERVER', pageNum=1, pageSize=100)))
                       if item['name'] == 'OSS HTTP ' + args.run_id)
        tenant.call('POST', '/admin/network/start/' + str(network['id']))
        status = tenant.call('GET', '/admin/http-access/networks/' + str(network['id']) + '/status')
        assert status['running']
        checks['http_network_validate_start_status'] = 'passed'
        report['test_data']['http']['network_id'] = network['id']
        stage = 'http_ingress'
        endpoint = '/iot/http/v2/' + access_code + '/report'
        ingress = Api()
        ingress.call('POST', endpoint, {'deviceId': devices['http']['deviceId'], 'temperature': 99.0},
                    expected=(401,))
        checks['http_ingress_rejects_missing_api_key'] = 'passed'
        http_value = sample_temperature(tenant, devices['http'], 27)
        report['observations']['http_temperature'] = http_value
        incoming = ingress.call('POST', endpoint, {'deviceId': devices['http']['deviceId'], 'temperature': http_value},
                               headers={'X-API-Key': api_key})
        assert incoming['success'] and incoming['acceptedRecords'] >= 1
        wait_for_value(tenant, devices['http'], http_value)
        wait_for_history(tenant, products['http'], devices['http'], http_value)
        checks['http_authenticated_ingress_decode_shadow_and_history_api_read'] = 'passed'
        stage = 'http_device_lifecycle'
        lifecycle_id = 'oss-http-' + args.run_id + '-lifecycle'
        lifecycle_items = rows(tenant.call('GET', query('/admin/v1/device/list', productKey=products['http']['productKey'], pageNum=1, pageSize=100)))
        lifecycle = next((item for item in lifecycle_items if item['deviceId'] == lifecycle_id), None)
        if lifecycle is None:
            tenant.call('POST', '/admin/v1/device', {'productKey': products['http']['productKey'],
                'deviceId': lifecycle_id, 'deviceName': 'Disposable HTTP lifecycle device'})
            lifecycle_items = rows(tenant.call('GET', query('/admin/v1/device/list', productKey=products['http']['productKey'], pageNum=1, pageSize=100)))
            lifecycle = next(item for item in lifecycle_items if item['deviceId'] == lifecycle_id)
        updated_name = 'Updated disposable HTTP lifecycle device'
        tenant.call('PUT', '/admin/v1/device', {'id': lifecycle['id'], 'productKey': products['http']['productKey'],
            'deviceId': lifecycle_id, 'deviceName': updated_name, 'detail': 'Updated through the public device API'})
        lifecycle_items = rows(tenant.call('GET', query('/admin/v1/device/list', productKey=products['http']['productKey'], pageNum=1, pageSize=100)))
        assert next(item for item in lifecycle_items if item['deviceId'] == lifecycle_id)['deviceName'] == updated_name
        tenant.call('DELETE', '/admin/v1/device/' + str(lifecycle['id']))
        lifecycle_items = rows(tenant.call('GET', query('/admin/v1/device/list', productKey=products['http']['productKey'], pageNum=1, pageSize=100)))
        assert not any(item['deviceId'] == lifecycle_id for item in lifecycle_items)
        checks['http_device_create_update_delete_lifecycle'] = 'passed'
        report['status'] = 'passed'
    except Exception as error:
        report.update({'status': 'failed', 'failed_stage': stage, 'error': str(error)[:500]})
        raise
    finally:
        report['finished_at'] = datetime.now(timezone.utc).isoformat()
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
        print(json.dumps({'status': report['status'], 'checks': checks, 'failed_stage': report.get('failed_stage')}, ensure_ascii=False))


if __name__ == '__main__':
    main()
