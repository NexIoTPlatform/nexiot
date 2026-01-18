<template>
  <div class="app-container">
    <a-card :bordered="false">
      <div class="page-header">
        <div class="header-left">
          <a-button
            type="text"
            icon="left"
            @click="back()"
            class="back-btn"
          />
          <div class="page-title">
            <h1>{{ devDetails.deviceName || '设备详情' }}（{{ devDetails.deviceId }}）</h1>
          </div>
        </div>
      </div>
      <a-spin :spinning="loading" tip="Loading...">
        <!-- 自定义标签页导航 -->
        <div class="custom-tabs-container">
          <div class="custom-tabs-nav">
            <div
              class="custom-tab-item"
              :class="{ active: activeTab === '1' }"
              @click="switchTab('1')"
            >
              设备信息
            </div>

            <div
              class="custom-tab-item"
              :class="{ active: activeTab === '2' }"
              @click="switchTab('2')"
            >
              实时状态
            </div>
            <div
              class="custom-tab-item"
              :class="{ active: activeTab === '3' }"
              @click="switchTab('3')"
            >
              物模型
            </div>
            <div
              class="custom-tab-item"
              :class="{ active: activeTab === '4' }"
              @click="switchTab('4')"
            >
              功能调用
            </div>
            <!-- <div
              class="custom-tab-item"
              :class="{ active: activeTab === '5' }"
              @click="switchTab('5')"
            >
              设备模拟
            </div> -->
            <div
              class="custom-tab-item"
              :class="{ active: activeTab === '6' }"
              @click="switchTab('6')"
            >
              设备影子
            </div>
            <div
              class="custom-tab-item"
              :class="{ active: activeTab === '7' }"
              @click="switchTab('7')"
            >
              日志查看
            </div>
            <div
              class="custom-tab-item"
              :class="{ active: activeTab === '8' }"
              @click="switchTab('8')"
            >
              消息订阅
            </div>
            <div
              v-if="deviceNode === 'GATEWAY'"
              class="custom-tab-item"
              :class="{ active: activeTab === '9' }"
              @click="switchTab('9')"
            >
              子设备
            </div>
            <div
              v-if="deviceNode === 'GATEWAY'"
              class="custom-tab-item"
              :class="{ active: activeTab === '10' }"
              @click="switchTab('10')"
            >
              云端轮询
            </div>
          </div>

          <!-- 标签页内容 -->
          <div class="custom-tab-content">
            <!-- 设备信息 -->
            <div v-show="activeTab === '1'" class="tab-pane">
              <!-- 基本信息区域 -->
              <div class="device-basic-info">
                <div class="basic-info-header">
                  <h3>设备基本信息</h3>
                </div>

                <div class="basic-info-grid">
                  <div class="info-item info-item-full">
                    <span class="info-label">设备名称</span>
                    <div class="info-value-group">
                      <span class="info-value">{{ devDetails.deviceName }}</span>
                      <a-button
                        type="text"
                        size="small"
                        class="copy-action-btn"
                        @click.stop="copyToClipboard(devDetails.deviceName)"
                        title="复制设备名称"
                      >
                        <a-icon type="copy"/>
                      </a-button>
                    </div>
                  </div>
                  <div class="info-item info-item-full">
                    <span class="info-label">所属产品</span>
                    <div class="info-value-group">
                      <span class="info-value info-link"
                            @click="$router.push(`/product/details/${productDetails.id}`)">{{
                          productDetails.productKey
                        }}</span>
                      <a-button
                        type="text"
                        size="small"
                        class="copy-action-btn"
                        @click.stop="copyToClipboard(productDetails.productKey)"
                        title="复制ProductKey"
                      >
                        <a-icon type="copy"/>
                      </a-button>
                    </div>
                  </div>
                  <div class="info-item info-item-full">
                    <span class="info-label">设备序列号</span>
                    <div class="info-value-group">
                      <span class="info-value">{{ devDetails.deviceId }}</span>
                      <a-button
                        type="text"
                        size="small"
                        class="copy-action-btn"
                        @click.stop="copyToClipboard(devDetails.deviceId)"
                        title="复制设备序列号"
                      >
                        <a-icon type="copy"/>
                      </a-button>
                    </div>
                  </div>
                  <div class="info-item info-item-full">
                    <span class="info-label">设备IotId</span>
                    <div class="info-value-group">
                      <span class="info-value">{{ devDetails.iotId }}</span>
                      <a-button
                        type="text"
                        size="small"
                        class="copy-action-btn"
                        @click.stop="copyToClipboard(devDetails.iotId)"
                        title="复制设备IotId"
                      >
                        <a-icon type="copy"/>
                      </a-button>
                    </div>
                  </div>

                  <div class="info-item info-item-full" v-if="devDetails.application">
                    <span class="info-label">归属应用</span>
                    <a-tag color="#7b90bb" size="small">{{ devDetails.application }}</a-tag>
                  </div>
                  <div class="info-item info-item-full" v-if="devDetails.detail">
                    <span class="info-label">备注</span>
                    <span class="info-value">{{ devDetails.detail }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">厂商简称</span>
                    <span class="info-value">{{ productDetails.companyNo }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">网络协议</span>
                    <span class="info-value">{{ productDetails.transportProtocol }}</span>
                  </div>
                  <div class="info-item" v-if="deviceNode === 'GATEWAY_SUB_DEVICE'">
                    <span class="info-label">所属网关设备</span>
                    <span class="info-value">{{ devDetails.gwDeviceInfo }}</span>
                  </div>
                  <div class="info-item ">
                    <span class="info-label">创建者</span>
                    <span class="info-value">{{ devDetails.creatorId }}</span>
                  </div>
                  <div class="info-item info-item-full">
                    <span class="info-label">{{ $t('app.createTime') }}</span>
                    <span class="info-value">{{ devDetails.createTime }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">注册时间</span>
                    <span class="info-value">{{ devDetails.registryTime }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">最后通信</span>
                    <span class="info-value">{{ devDetails.onlineTime }}</span>
                  </div>
                  <div class="info-item info-item-full">
                    <span class="info-label">设备位置</span>
                    <div class="info-value-group">
                      <span class="info-value">{{ locationStr || '未设置' }}</span>
                      <a-button
                        v-if="locationStr"
                        type="text"
                        size="small"
                        class="map-action-btn"
                        @click="showMapModal"
                        title="查看地图"
                      >
                        <a-icon type="environment"/>
                      </a-button>
                    </div>
                  </div>
                </div>
                <div class="info-item info-item-full" v-if="devDetails.devGroupName.length>0">
                  <span class="info-label">{{ $t('device.deviceGroup') }}</span>
                  <div class="tag-group">
                    <a-tag v-for="item in devDetails.devGroupName" :key="item" color="#6ba0b3"
                           size="small">
                      {{ item }}
                    </a-tag>
                  </div>
                </div>

              </div>

              <!-- 地图区域 -->
              <!-- <div class="device-location-info">
                <div class="location-header">
                  <h3>设备位置</h3>
                  <div class="location-coords">
                    <span>坐标：{{ locationStr || '未设置' }}</span>
                  </div>
                </div>
                <div class="map-container">
                  <div id="showmap-container"></div>
                </div>
              </div> -->
            </div>

            <!-- 物模型 -->
            <div v-show="activeTab === '3'" class="tab-pane">
              <!-- 物模型组件 -->
              <div class="metadata-wrapper">
                <metadata
                  v-if="productDetails.id && productDetails.productKey"
                  ref="metadata"
                  :product-id="productDetails.id"
                  :creator-id="productDetails.creatorId"
                  :product-key="productDetails.productKey"
                  :third-platform="productDetails.thirdPlatform"
                  @open-metadata-detail="openMetadataDetail"
                />
              </div>
            </div>

            <!-- 设备运行状态 -->
            <div v-show="activeTab === '2'" class="tab-pane">
              <div class="status-container">
                <!-- 设备属性 -->
                <div class="status-header">
                  <div class="status-title">
                    <span class="title-text">设备属性</span>
                    <a-badge
                      v-if="properties && properties.length > 0"
                      :count="properties.length"
                      :number-style="{ backgroundColor: '#1890ff' }"
                    />
                    <!-- 智能提示信息 -->
                    <div class="smart-tip" v-if="showSmartTip" :data-type="smartTipType">
                      <a-tooltip placement="top" :title="getSmartTipContent()">
                        <a-icon type="info-circle" class="tip-icon"/>
                        <span class="tip-text">{{ getSmartTipText() }}</span>
                      </a-tooltip>
                    </div>
                  </div>
                  <div class="status-actions">
                    <a-button
                      class="refresh-btn"
                      :loading="loading"
                      @click="refreshStatus"
                    >
                      <a-icon type="sync" :spin="loading"/>
                    </a-button>
                    <a-switch
                      v-model="autoRefresh"
                      @change="toggleAutoRefresh"
                      size="small"
                    >
                      <span slot="checkedChildren">自动</span>
                      <span slot="unCheckedChildren">手动</span>
                    </a-switch>
                  </div>
                </div>

                <!-- 属性卡片显示 -->
                <div v-if="properties && properties.length > 0" class="status-cards">
                  <div class="status-card property-card"
                       :class="{ 'desired-card': item.customized === 'device_desired_property' }"
                       v-for="(item, index) in properties"
                       :key="index"
                       v-copy="item.formatValue || item.desireFormatValue || item.desireValue || '--'"
                       :title="item.formatValue && item.desireValue != null && String(item.desireValue) !== String(item.value)
                         ? ('上报值：' + String(item.formatValue) + '，期望值：' + String(item.desireValue))
                         : (!item.formatValue && item.desireValue != null
                           ? ('期望值：' + String(item.desireValue) + '（设备未上报）')
                           : String(item.formatValue || item.desireFormatValue || item.desireValue || '--'))"
                  >
                    <div class="card-header">
                      <div class="card-title" :title="item.name + '(' + item.property + ')'">
                        {{ item.name }}
                        <span class="property-key">({{ item.property }})</span>
                      </div>
                      <div class="card-badge badge-property" :class="{ 'has-desire': item.customized === 'device_desired_property' }">
                        <span class="badge-dot" :style="item.customized === 'device_desired_property' ? 'color:#38aa39' : ''">●</span>
                        <span class="badge-text">属性</span>
                      </div>
                    </div>
                    <div class="card-value property-value">
                      <div
                        class="value-row"
                      >
                        <a-tooltip
                          placement="top"
                          :title="item.formatValue && item.desireValue != null && String(item.desireValue) !== String(item.value)
                            ? ('上报值：' + String(item.formatValue) + '，期望值：' + String(item.desireValue))
                            : (!item.formatValue && item.desireValue != null
                              ? ('期望值：' + String(item.desireValue) + '（设备未上报）')
                              : String(item.formatValue || item.desireValue || '--'))"
                        >
                          <span class="value-text">{{ item.formatValue || item.desireValue || '--' }}</span>
                        </a-tooltip>
                      </div>
                    </div>
                    <div class="card-footer">
                      <div class="card-time">
                        <a-icon type="clock-circle"/>
                        {{ item.lastTime }}
                      </div>
                      <div class="card-actions">
                        <!-- 地理位置类型显示轨迹按钮 -->
                        <div
                          v-if="item.storagePolicy && isGeoPointType(item)"
                          class="history-btn track-btn"
                          @click="showTrackMap(item)"
                          title="查看轨迹地图"
                        >
                          <a-icon type="environment" />
                          <span class="history-text">轨迹</span>
                        </div>
                        <!-- 历史按钮（地理位置类型也显示） -->
                        <div
                          v-if="item.storagePolicy"
                          class="history-btn"
                          @click="lookMeta(item, 'PROPERTIES')"
                          title="查看历史数据"
                        >
                          <a-icon type="history" />
                          <span class="history-text">历史</span>
                        </div>
                         <div
                           class="copy-btn"
                           @click.stop="copyValue(item.formatValue || item.desireValue)"
                           title="复制数值"
                         >
                           <a-icon type="copy"/>
                         </div>
                      </div>
                    </div>
                  </div>
                </div>

                <div v-else class="status-cards">
                  <div class="status-card empty-state">
                    <div class="empty-text">该设备目前没有属性数据</div>
                    <div class="empty-action">
                      <a-button type="primary" ghost @click="refreshStatus">
                        <a-icon type="sync"/>
                        重新获取
                      </a-button>
                    </div>
                  </div>
                </div>

                <!-- 设备事件 -->
                <div v-if="isEventShow" style="margin-top: 40px;">
                  <div class="status-header">
                    <div class="status-title event-title">
                      设备事件
                      <a-badge :count="events.length"
                               :number-style="{ backgroundColor: '#fa8c16' }"/>
                    </div>
                    <div class="status-actions">
                      <a-select
                        v-model="eventFilter"
                        size="small"
                        style="width: 120px; margin-right: 8px;"
                        @change="filterEvents"
                      >
                        <a-select-option value="all">全部事件</a-select-option>
                        <a-select-option value="warn">告警事件</a-select-option>
                        <a-select-option value="info">信息事件</a-select-option>
                      </a-select>
                      <a-button
                        class="refresh-btn"
                        :loading="loading"
                        @click="refreshStatus"
                      >
                        <a-icon type="sync" :spin="loading"/>
                      </a-button>
                    </div>
                  </div>

                  <div class="status-cards" v-if="filteredEvents.length > 0">
                    <div class="status-card event-card" v-for="(item, index) in filteredEvents"
                         :key="index">
                      <div class="card-header">
                        <div class="card-title" :title="item.name + '(' + item.id + ')'">
                          {{ item.name }}
                          <span class="event-key">({{ item.id }})</span>
                        </div>
                        <div class="card-badge badge-event" :class="getEventBadgeClass(item)">
                          <span class="badge-dot">●</span>
                          {{ getEventLevel(item) }}
                        </div>
                      </div>
                      <div class="card-value event-value" :title="getEventDescription(item)">
                        <div class="event-count">{{ (item.qty ? item.qty : 0) }}次</div>
                        <div class="event-description">{{ getEventDescription(item) }}</div>
                      </div>
                      <div class="card-footer">
                        <div class="card-time">
                          <a-icon type="clock-circle"/>
                          {{ item.lastTime }}
                        </div>
                        <div class="card-actions">
                          <div
                            v-if="item.storagePolicy"
                            class="history-btn"
                            @click="lookMeta(item, 'EVENT')"
                            title="查看历史数据"
                          >
                            <a-icon type="history" />
                            <span class="history-text">历史</span>
                          </div>
                          <!-- <div
                            class="detail-btn"
                            @click="showEventDetail(item)"
                            title="查看详情"
                          >
                            <a-icon type="eye" />
                          </div> -->
                        </div>
                      </div>
                    </div>
                  </div>

                  <div v-else class="status-cards">
                    <div class="status-card empty-state">
                      <div class="empty-text">{{
                          eventFilter === 'all' ? '该设备目前没有事件数据' : '没有符合条件的事件'
                        }}
                      </div>
                      <div class="empty-action">
                        <a-button type="primary" ghost @click="refreshStatus">
                          <a-icon type="reload"/>
                          重新获取
                        </a-button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 指令下发 -->
            <div v-show="activeTab === '4'" class="tab-pane">
              <!-- <div class="simple-header">
                <h3>功能下发</h3>
                <p>向设备下发功能指令，控制设备执行特定操作</p>
              </div> -->
              <div v-if="metaData.functions.length <= 0" class="warning-message">
                <span>该设备暂无可用功能，请检查物模型配置</span>
              </div>
              <!-- 功能下发弹窗 -->
              <function-down
                v-if="deviceNo && productKey"
                :meta-data="metaData"
                :device-no="deviceNo"
                :configration="configuration"
                :product-key="productKey"
                :source="source"
                :show="showFunctionDown"
                :type="functionType"
                @close="closeFunctionDown"
              />
            </div>
            <!-- 设备影子 -->
            <div v-show="activeTab === '6'" class="tab-pane">
              <div class="simple-header">
                <h3>设备影子</h3>
              </div>

              <div class="shadow-basic-info">
                <div class="shadow-item">
                  <span class="shadow-label">设备序列号</span>
                  <a-tag color="#108ee9">{{ deviceShadow.deviceId }}</a-tag>
                </div>
                <div class="shadow-item">
                  <span class="shadow-label">最后通信时间</span>
                  <a-tag color="#87d068">{{ deviceShadow.lastTime }}</a-tag>
                </div>
              </div>

              <div class="shadow-data-section">
                <h4>影子数据</h4>
                <json-viewer
                  :value="deviceShadow.metadatas"
                  :expand-depth="5"
                  copyable
                  style="background: #f8fafc; border-radius: 8px; padding: 16px; border: 1px solid #e2e8f0;"
                >
                  <template #copy>
                    <a-button size="small" type="primary" ghost>
                      <i class="anticon anticon-copy"></i>
                      复制
                    </a-button>
                  </template>
                </json-viewer>
              </div>
            </div>

            <!-- 设备日志管理 -->
            <div v-show="activeTab === '7'" class="tab-pane">
              <div class="simple-header">
                <h3>日志管理</h3>
                <p>查看和管理设备运行日志，监控设备状态和异常信息</p>
              </div>
              <log-manage
                v-if="devDetails.productKey && devDetails.iotId"
                ref="LogManage"
                :product-key="devDetails.productKey"
                :dev-id="devDetails.iotId"
                :device-id="devDetails.deviceId"
              />
            </div>

            <!-- 设备订阅管理 -->
            <div v-show="activeTab === '8'" class="tab-pane">
              <div class="simple-header">
                <h3>订阅管理</h3>
                <p>管理设备数据订阅，配置数据推送和消息通知</p>
              </div>
              <subscribe
                v-if="devDetails.productKey && devDetails.iotId"
                ref="subscribe"
                :product-key="devDetails.productKey"
                :dev-id="devDetails.iotId"
              />
            </div>

            <!-- 子设备 -->
            <div v-show="activeTab === '9'" class="tab-pane" v-if="deviceNode === 'GATEWAY'">
              <!-- <div class="simple-header">
                <h3>子设备管理</h3>
                <p>管理网关下的子设备，查看子设备状态和配置信息</p>
              </div> -->
              <!-- 根据传输协议判断显示哪个组件 -->
              <ModbusSubDeviceList
                v-if="devDetails.deviceId && devDetails.productKey && isModbusGateway"
                :gwDeviceId="devDetails.deviceId"
                :gwProductKey="devDetails.productKey"
                :gatewayInfo="devDetails"
                :gatewayProductInfo="productDetails"

              />
              <ChildDevice
                v-else-if="devDetails.deviceId && devDetails.productKey"
                :devId="devDetails.deviceId"
                :extDeviceId="devDetails.deviceId"
                :gwProductKey="devDetails.productKey"
              />
            </div>

            <!-- 云端轮询 -->
            <div v-show="activeTab === '10'" class="tab-pane" v-if="deviceNode === 'GATEWAY'">
              <gateway-polling-config
                v-if="devDetails.deviceId && devDetails.productKey"
                :device-info="devDetails"
              />
            </div>
          </div>
        </div>
      </a-spin>
      <!--日志列表-->
      <device-data-trend
        v-if="devId && devDetails.productKey"
        :dev-id="devId"
        :show="showMetaLog"
        :look-data="lookData"
        :type="metaType"
        :meta="metaName"
        :meta-id="metaId"
        :product-key="devDetails.productKey"
        :symbol="metaSymbol"
        @close="closeMetaLog"
      />
      <!-- 轨迹地图弹窗 -->
      <map-track-modal
        v-if="devId && devDetails.productKey"
        :show="showTrackModal"
        :device-id="devId"
        :product-key="devDetails.productKey"
        :meta-id="trackMetaId"
        :meta-name="trackMetaName"
        @close="closeTrackModal"
      />
      <!-- 物模型详细信息 -->
      <metadata-show
        v-if="productDetails.id"
        ref="metadataShow"
        :product-id="productDetails.id"
        :show="metadataShow"
        :gid="productDetails.configuration"
        :type="metadataShowType"
        @close="closeMetadataDetail"
      />
      <!-- 增加修改 -->
      <create-form ref="createForm" :stateOptions="stateOptions" @ok="getList"/>
    </a-card>

    <a-modal
      v-model="mapModalVisible"
      title="设备位置"
      :footer="null"
      centered
      width="600px"
      :forceRender="true"
      @visible-change="onMapModalVisibleChange"
    >
      <div id="showmap-modal-container" style="width:100%;height:400px;"></div>
    </a-modal>
  </div>
</template>
<script>
import { getEventTotal } from '@/api/system/dev/deviceLog'
import { getInstance } from '@/api/system/dev/instance'
import { getProByKey } from '@/api/system/dev/product'
import { devListShadow, getDeviceShadow } from '@/api/system/dev/shadow'
import { toDate } from '@/utils/date'
import AMapLoader from '@amap/amap-jsapi-loader'
import { Modal } from 'ant-design-vue'
import JsonViewer from 'vue-json-viewer'
import ChildDevice from './ChildDevice'
import CreateForm from './CreateForm'
import DeviceDataTrend from './DeviceDataTrend'
import DeviceDebugging from './DeviceDebugging'
import FunctionDown from './FunctionDown'
import GatewayPollingConfig from './GatewayPollingConfig'
import LogManage from './logManage'
import MapTrackModal from './MapTrackModal'
import metadata from './metadata'
import metadataShow from './metadataShow'
import ModbusSubDeviceList from './ModbusSubDeviceList'
import SIMCard from './SIMCard'
import Subscribe from './Subscribe'

export default {
  name: 'InstanceDetails',
  components: {
    JsonViewer,
    LogManage,
    DeviceDataTrend,
    MapTrackModal,
    CreateForm,
    FunctionDown,
    DeviceDebugging,
    metadata,
    metadataShow,
    Map,
    Subscribe,
    SIMCard,
    ChildDevice,
    ModbusSubDeviceList,
    GatewayPollingConfig
  },
  props: [],
  data() {
    return {
      // 物模型详情是否显示
      metadataShow: false,
      // 物模型显示类型 type === 1 IoT物模型；type === 2 的物模型
      metadataShowType: 1,
      isEventShow: false,
      // 地图显示
      showMap: null,
      // 经纬度
      location: {
        lat: null,
        lng: null
      },
      // 经纬度
      locationStr: undefined,
      // 设备节点
      deviceNode: '',
      // 间距大小
      spanSize: 10,
      // 0-离线，1-在线字典
      stateOptions: [],
      // 加载控制开关
      loading: false,
      // 设备id
      deviceId: undefined,
      // 设备序列号
      deviceNo: undefined,
      // 配置信息
      configuration: undefined,
      // productKey
      productKey: undefined,
      // source
      source: undefined,
      // 设备详细信息
      devDetails: {
        id: null,

        iotId: null,

        deviceId: null,

        extDeviceId: null,

        deriveMetadata: null,

        configuration: null,

        productName: null,

        nickName: null,

        features: null,

        parentId: null,

        orgId: null,

        productKey: null,

        secretKey: null,

        deviceName: null,

        creatorId: null,

        instance: null,

        application: null,

        creatorName: null,

        state: null,

        detail: null,

        createTime: null,

        registryTime: null,

        onlineTime: null,

        areasId: null,

        hasProtocol: false,
        devGroupName: [],
        gwDeviceInfo: null
      },
      // 物模型
      metaData: {
        functions: []
      },
      lookData: {},
      // 产品详细信息
      productDetails: {
        id: undefined,
        productId: undefined,
        productKey: undefined,
        productSecret: undefined,
        thirdPlatform: undefined,
        thirdConfiguration: undefined,
        companyNo: undefined,
        classifiedId: undefined,
        configuration: undefined,
        networkWay: undefined,
        deviceNode: undefined,
        projectName: undefined,
        projectId: undefined,
        classifiedName: undefined,
        messageProtocol: undefined,
        orgId: undefined,
        name: undefined,
        creatorId: undefined,
        describe: undefined,
        storePolicy: undefined,
        storePolicyConfiguration: undefined,
        transportProtocol: undefined,
        photoUrl: undefined,
        protocolName: undefined,
        metadata: undefined,
        state: undefined

      },
      // 设备影子
      deviceShadow: {
        deviceId: undefined,
        lastTime: undefined,
        metadatas: ''
      },
      // 属性状态
      properties: [],
      // 事件状态
      events: [],
      // 自动刷新开关
      autoRefresh: false,
      // 自动刷新定时器
      refreshTimer: null,
      // 事件过滤器
      eventFilter: 'all',
      // 日志详情显示状态
      showMetaLog: false,
      // 属性/事件名称
      metaName: '',
      // 属性/事件类型
      metaType: '',
      // 智能提示相关
      showSmartTip: false,
      smartTipType: 'default', // default, noData, dataTypeMismatch, jsonError
      // 属性/事件的名称标识
      metaId: '',
      // 属性符号
      metaSymbol: '',
      // iotId
      devId: undefined,
      // 功能下发显示状态
      showFunctionDown: false,
      // 在线模拟显示状态
      showDeviceDebugging: false,
      // 下发/调试
      functionType: '',
      iccid: '',
      activeTab: '1',
      mapModalVisible: false,
      // 轨迹地图弹窗相关
      showTrackModal: false,
      trackMetaId: '',
      trackMetaName: '',
      mapInstance: null,
      mapInitToken: 0,
      alertList: [
        {id: 1, title: '温度过高', level: 'error', time: '21:30:12'},
        {id: 2, title: '电量低', level: 'warning', time: '20:12:05'},
        {id: 3, title: '离线告警', level: 'default', time: '19:55:33'}
      ]
    }
  },
  watch: {
    activeTab(newVal) {
      if (newVal === '4') {
        this.showFunctionDown = true;
      }
      if (newVal === '5') {
        this.showDeviceDebugging = true;
      }
    },
  },
  computed: {
    // 过滤后的事件
    filteredEvents() {
      if (!this.events || this.events.length === 0) {
        return []
      }

      if (this.eventFilter === 'all') {
        return this.events
      }

      return this.events.filter(event => {
        const eventName = event.name.toLowerCase()
        if (this.eventFilter === 'warn') {
          return eventName.includes('告警') || eventName.includes('警告') || eventName.includes(
            '异常')
        } else if (this.eventFilter === 'info') {
          return !eventName.includes('告警') && !eventName.includes('警告') && !eventName.includes(
            '异常')
        }
        return true
      })
    },
    // 判断是否为Modbus网关
    isModbusGateway() {
      if (!this.productDetails || !this.productDetails.transportProtocol) {
        return false
      }
      const isModbus = this.productDetails.transportProtocol.toLowerCase() === 'modbus'
      return isModbus
    }
  },
  async created() {
    this.deviceId = this.$route.params.id

    // 验证路由参数
    if (!this.deviceId) {
      console.error('设备iotId参数缺失')
      this.$message.error('设备iotId参数缺失')
      this.$router.push('/index')
      return
    }

    try {
      // 获取设备详情
      await this.getDevDeatils()

      // 获取字典数据
      await this.getDicts('dev_instance_state').then((response) => {
        if (response && response.data) {
          this.stateOptions = response.data
        }
      })

      // 初始化地图（延迟到DOM渲染完成后）
      this.$nextTick(() => {
        this.initMap()
      })
    } catch (e) {
      console.error('初始化页面失败:', e)
      this.$message.error('初始化页面失败')
    }
  },
  methods: {
    // 智能提示相关方法
    /**
     * 判断是否显示智能提示
     */
    shouldShowSmartTip() {
      // 如果没有属性数据，显示提示
      if (!this.properties || this.properties.length === 0) {
        this.smartTipType = 'noData'
        this.showSmartTip = true
        return
      }

      // 检查是否有数据类型不匹配的问题
      const hasDataTypeMismatch = this.properties.some(prop => {
        return prop.formatValue && prop.formatValue !== '--' &&
          (prop.dataType && prop.dataType !== prop.expectedType)
      })

      if (hasDataTypeMismatch) {
        this.smartTipType = 'dataTypeMismatch'
        this.showSmartTip = true
        return
      }

      // 检查是否有JSON解析错误
      const hasJsonError = this.properties.some(prop => {
        return prop.formatValue && prop.formatValue.includes('JSON') &&
          prop.formatValue.includes('错误')
      })

      if (hasJsonError) {
        this.smartTipType = 'jsonError'
        this.showSmartTip = true
        return
      }

      // 默认情况下隐藏提示
      this.showSmartTip = false
    },

    /**
     * 获取智能提示文本
     */
    getSmartTipText() {
      switch (this.smartTipType) {
        case 'noData':
          return '暂无数据'
        case 'dataTypeMismatch':
          return '数据类型不匹配'
        case 'jsonError':
          return 'JSON格式错误'
        default:
          return '数据异常'
      }
    },

    /**
     * 获取智能提示详细内容
     */
    getSmartTipContent() {
      switch (this.smartTipType) {
        case 'noData':
          return '该设备目前没有上报属性数据，请检查设备是否正常连接或是否已配置物模型。'
        case 'dataTypeMismatch':
          return '设备上报的数据类型与物模型定义不一致，请检查数据类型是否匹配。'
        case 'jsonError':
          return '设备上报的JSON格式有误，请检查数据格式是否符合规范。'
        default:
          return '若设备上报数据后界面未能展示最新数据，请检查：1. 上报JSON格式是否正确；2. 数据类型是否与物模型定义保持一致；3. 网络连接是否正常。'
      }
    },

    // 初始化地图
    initMap() {
      AMapLoader.load({
        key: "76aa92312d6a00bcf0a92e2c11b509e0", // 申请好的Web端开发者Key，首次调用 load 时必填
        version: "2.0", // 指定要加载的 JSAPI 的版本，缺省时默认为 1.4.15
        plugins: ["AMap.Geocoder", "AMap.AutoComplete", "AMap.Scale"], // 需要使用的的插件列表，如比例尺'AMap.Scale'等
        AMapUI: {
          // 是否加载 AMapUI，缺省不加载
          version: "1.1", // AMapUI 版本
          plugins: ["overlay/SimpleMarker"], // 需要加载的 AMapUI ui 插件
        },
        Loca: {
          // 是否加载 Loca， 缺省不加载
          version: "2.0", // Loca 版本
        },
      })
        .then((AMap) => {
          window.AMap = AMap
          this.map = new AMap.Map('showmap-modal-container', {
            zoom: 12,
            center: [81.368244, 44.620889]
          })

          // 添加比例尺组件到地图实例上
          this.map.addControl(new AMap.Scale())

          this.geocoder = new AMap.Geocoder()
          this.autoComplete = new AMap.AutoComplete()

          // 如果有位置信息，绘制标点
          if (this.location.lng && this.location.lat) {
            this.drawPoint()
            this.map.panTo([this.location.lng, this.location.lat])
          }
        })
        .catch((e) => {
          console.error('地图初始化失败:', e) // 加载错误提示
        })
    },

    drawPoint() {
      if (!this.map || !this.location.lng || !this.location.lat) {
        return
      }

      if (this.marker) {
        this.map.remove(this.marker)
      }
      this.marker = new AMap.Marker({
        position: new AMap.LngLat(this.location.lng, this.location.lat)
      })
      this.map.add(this.marker)
    },
    // 获取设备详情
    async getDevDeatils() {
      // 验证设备iotId
      if (!this.deviceId) {
        console.error('设备iotId为空')
        this.$message.error('设备iotId不能为空')
        this.$router.push(`/index`)
        return
      }

      this.loading = true
      try {
        const res = await getInstance(this.deviceId)

        if (!res.data) {
          throw new Error('设备数据为空')
        }

        // iccid
        console.log('res.data', res.data)
        if (res.data.configuration) {
          try {
            const json = JSON.parse(res.data.configuration)
            this.iccid = json.iccid
          } catch (e) {
            console.log('解析配置信息失败:', e)
          }
        }

        // 处理位置信息
        if (res.data.params && res.data.params.locationStr) {
          const coords = res.data.params.locationStr.split(',')
          if (coords.length === 2) {
            this.location.lng = coords[0]
            this.location.lat = coords[1]
            this.locationStr = this.location.lng + ',' + this.location.lat
          }
        } else {
          this.location = {}
          this.locationStr = null
        }

        this.hasProtocol = res.data.hasProtocol
        this.devDetails = res.data
        this.configuration = this.devDetails.configuration
        this.devId = this.devDetails.iotId
        this.deviceNo = this.devDetails.deviceId

        // 统一处理时间字段格式
        this.normalizeDevTimeFields(this.devDetails)
        if (this.devDetails.devGroupName) {
          this.devDetails.devGroupName = res.data.devGroupName
        }

        // 获取设备影子信息 - 添加参数验证
        if (this.devDetails.iotId) {
          getDeviceShadow(this.devDetails.iotId).then((response) => {
            if (response && response.data) {
              this.deviceShadow.lastTime = this.formatTimeValue(response.data.lastTime)
              this.deviceShadow.deviceId = response.data.deviceId
              try {
                this.deviceShadow.metadatas = JSON.parse(response.data.metadata)
              } catch (e) {
                console.log('解析设备影子数据失败:', e)
                this.deviceShadow.metadatas = {}
              }
            }
          }).catch((e) => {
            console.log('获取设备影子信息失败:', e)
          })
        }

        // 获取设备产品信息 - 添加参数验证
        if (this.devDetails.productKey) {
          getProByKey(this.devDetails.productKey).then((res) => {
            if (res && res.data) {
              this.productDetails = res.data
              this.source = this.productDetails.thirdPlatform
              this.deviceNode = this.productDetails.deviceNode
              this.productKey = this.productDetails.productKey

              // 获取设备事件统计信息 - 添加参数验证
              if (this.productDetails.productKey && this.devDetails.iotId) {
                this.devEventShadow(this.productDetails.productKey, this.devDetails.iotId)
              }

              if (this.productDetails.metadata) {
                try {
                  this.metaData = JSON.parse(this.productDetails.metadata)
                  // 防止function为空
                  if (!this.metaData.functions) {
                    this.metaData.functions = []
                  }
                } catch (e) {
                  console.log('解析物模型数据失败:', e)
                  this.metaData = {functions: []}
                }
              }
            }
          }).catch((e) => {
            console.log('获取产品信息失败:', e)
          })
        } else {
          console.warn('产品Key为空，跳过获取产品信息')
        }

        // 获取设备属性状态信息 - 添加参数验证
        if (this.devDetails.iotId) {
          this.devPropertiesShadow(this.devDetails.iotId)
        }
      } catch (e) {
        console.error('获取设备详情失败:', e)
        this.$message.error('获取设备详情失败')
        this.$router.push(`/index`)
      } finally {
        this.loading = false
      }
    },
    // 刷新运行状态
    refreshStatus() {
      // 参数验证
      if (!this.productKey || !this.devId) {
        this.$message.warning('设备信息不完整，无法刷新状态')
        return
      }

      this.devEventShadow(this.productKey, this.devId)
      this.devPropertiesShadow(this.devId)

      // 延迟执行智能提示判断，确保数据已更新
      this.$nextTick(() => {
        this.shouldShowSmartTip()
      })

      this.$message.success('刷新成功', 2)
    },

    // 切换自动刷新
    toggleAutoRefresh(enabled) {
      if (enabled) {
        this.refreshTimer = setInterval(() => {
          this.refreshStatus()
        }, 30000) // 30秒刷新一次
        this.$message.success('已开启自动刷新（30秒间隔）')
      } else {
        if (this.refreshTimer) {
          clearInterval(this.refreshTimer)
          this.refreshTimer = null
        }
        this.$message.info('已关闭自动刷新')
      }
    },

    // 复制数值
    copyValue(value) {
      this.$copyText(value).then(() => {
        this.$message.success('已复制到剪贴板')
      }).catch(() => {
        this.$message.error('复制失败')
      })
    },

    // 获取事件级别
    getEventLevel(item) {
      const eventName = item.name.toLowerCase()
      if (eventName.includes('告警') || eventName.includes('警告') || eventName.includes('异常')) {
        return '告警'
      }
      return '信息'
    },

    // 获取事件徽章样式
    getEventBadgeClass(item) {
      const eventName = item.name.toLowerCase()
      if (eventName.includes('告警') || eventName.includes('警告') || eventName.includes('异常')) {
        return 'badge-warning'
      }
      return 'badge-info'
    },

    // 获取事件描述
    getEventDescription(item) {
      return item.name || '无描述'
    },

    // 显示事件详情
    showEventDetail(item) {
      Modal.info({
        title: '事件详情',
        content: h => h('div', {class: 'instance-event-modal-content'}, [
          h('div', {class: 'instance-event-row'}, [
            h('span', {class: 'instance-event-label'}, '事件名称：'),
            h('span', {class: 'instance-event-value'}, item.name)
          ]),
          h('div', {class: 'instance-event-row'}, [
            h('span', {class: 'instance-event-label'}, '事件ID：'),
            h('span', {class: 'instance-event-value'}, item.id)
          ]),
          h('div', {class: 'instance-event-row'}, [
            h('span', {class: 'instance-event-label'}, '触发次数：'),
            h('span', {class: 'instance-event-value'}, `${item.qty || 0}次`)
          ]),
          h('div', {class: 'instance-event-row'}, [
            h('span', {class: 'instance-event-label'}, '最后触发：'),
            h('span', {class: 'instance-event-value'}, item.lastTime)
          ]),
          h('div', {class: 'instance-event-row'}, [
            h('span', {class: 'instance-event-label'}, '事件描述：'),
            h('span', {class: 'instance-event-value'}, this.getEventDescription(item))
          ])
        ]),
        width: 500
      })
    },

    // 过滤事件
    filterEvents() {
      // 触发计算属性重新计算
      this.$forceUpdate()
    },
    // 获取设备事件统计信息
    devEventShadow(productKey, iotId) {
      // 参数验证
      if (!productKey || !iotId) {
        console.warn('获取设备事件统计信息：参数不完整', {productKey, iotId})
        return
      }

      getEventTotal(productKey, iotId).then((res) => {
        if (res && res.rows) {
          res.rows.forEach((item) => {
            item.lastTime = this.formatTimeValue(item.time)
          })
          this.events = res.rows
          this.isEventShow = true
        }
      }).catch((e) => {
        console.log('获取设备事件统计信息失败:', e)
      })
    },
    // 获取设备属性状态信息
    devPropertiesShadow(iotId) {
      // 参数验证
      if (!iotId) {
        console.warn('获取设备属性状态信息：iotId为空')
        return
      }

      devListShadow(iotId).then((res) => {
        if (res && res.data) {
          res.data.forEach((item) => {
            item.name = item.propertyName
            item.lastTime = this.formatTimeValue(item.timestamp)
          })
          this.properties = res.data
        }
      }).catch((e) => {
        console.log('获取设备属性状态信息失败:', e)
      })
    },
    // 规范化设备详情中的时间字段
    normalizeDevTimeFields(dev) {
      if (!dev || typeof dev !== 'object') return
      // registryTime 兼容 activeTime
      if (!dev.registryTime && dev.activeTime) {
        dev.registryTime = dev.activeTime
      }
      const timeKeys = [
        'createTime',
        'onlineTime',
        'registryTime',
        'lastTime',
        'updateDate'
      ]
      timeKeys.forEach((key) => {
        if (dev[key]) {
          dev[key] = this.formatTimeValue(dev[key])
        }
      })
    },
    // 将任意时间值格式化为字符串
    formatTimeValue(value) {
      if (!value) return '无'
      const d = toDate(value)
      if (isNaN(d.getTime())) return '无'
      return this.formatDate(d)
    },
    // 时间转换
    formatDate(time) {
      // 获取年
      const year = time.getFullYear()
      // 获取月
      const month = time.getMonth() + 1
      // 获取日
      const date = time.getDate()
      // 获取星期
      // eslint-disable-next-line no-unused-vars
      const day = time.getDay()
      // 获取小时
      const hours = time.getHours()
      // 获取分钟
      const minutes = time.getMinutes()
      // 获取秒
      const seconds = time.getSeconds()
      // 获取毫秒
      // eslint-disable-next-line no-unused-vars
      const ms = time.getMilliseconds()
      let curDateTime = year
      if (month > 9) {
        curDateTime = curDateTime + '-' + month
      } else {
        curDateTime = curDateTime + '-0' + month
      }
      if (date > 9) {
        curDateTime = curDateTime + '-' + date
      } else {
        curDateTime = curDateTime + '-0' + date
      }
      if (hours > 9) {
        curDateTime = curDateTime + ' ' + hours
      } else {
        curDateTime = curDateTime + ' 0' + hours
      }
      if (minutes > 9) {
        curDateTime = curDateTime + ':' + minutes
      } else {
        curDateTime = curDateTime + ':0' + minutes
      }
      if (seconds > 9) {
        curDateTime = curDateTime + ':' + seconds
      } else {
        curDateTime = curDateTime + ':0' + seconds
      }
      return curDateTime
    },
    // 返回上一级
    back() {
      this.$router.back()
    },
    // 查看事件或者设备详细日志
    lookMeta(item, metaType) {
      this.showMetaLog = true
      this.metaName = item.name
      this.metaType = metaType
      this.lookData = item
      if (metaType === 'EVENT') {
        this.metaId = item.id
        this.metaSymbol = ''
      } else {
        this.metaId = item.property
        this.metaSymbol = item.symbol
      }
    },
    // 判断是否为地理位置类型
    isGeoPointType(item) {
      return item.type === 'geoPoint' || item.type === 'geo_point'
    },
    // 显示轨迹地图
    showTrackMap(item) {
      this.trackMetaId = item.property
      this.trackMetaName = item.name
      this.showTrackModal = true
    },
    // 关闭轨迹地图弹窗
    closeTrackModal() {
      this.showTrackModal = false
    },
    // 关闭查看事件或者设备详细日志
    closeMetaLog() {
      this.showMetaLog = false
    },
    // 关闭功能下发
    closeFunctionDown() {
      this.showFunctionDown = false
    },
    // 关闭设备调试
    closeDeviceDebugging() {
      this.showDeviceDebugging = false
    },
    // 刷新设备详情（避免重复请求）
    getList() {
      // 只在必要时刷新，避免重复请求
      if (!this.loading) {
        console.log('刷新设备详情')
        this.getDevDeatils()
      }
    },
    // 功能下发
    functionDown() {
      this.showFunctionDown = true;
    },
    // 设备调试
    deviceDebugging() {
      this.showDeviceDebugging = true;
    },
    // 查看地图
    checkMap() {
      if (this.showMap) {
        this.showMap = false
      } else {
        this.showMap = true
      }
    },
    // 关闭地图弹出层
    closeMap() {
      this.showMap = false
    },
    // 关闭物模型详情
    closeMetadataDetail() {
      this.metadataShow = false
    },
    // 打开物模型详情
    openMetadataDetail() {
      if (this.metadataShow) {
        this.metadataShowType = 1
        this.metadataShow = false
      } else {
        this.metadataShowType = 1
        this.metadataShow = true
      }
    },

    switchTab(tab) {
      this.activeTab = tab
    },
    // 复制到剪贴板
    copyToClipboard(text) {
      if (navigator.clipboard) {
        navigator.clipboard.writeText(text).then(() => {
          this.$message.success('已复制到剪贴板')
        }).catch(() => {
          this.fallbackCopyTextToClipboard(text)
        })
      } else {
        this.fallbackCopyTextToClipboard(text)
      }
    },
    // 兜底复制方法
    fallbackCopyTextToClipboard(text) {
      const textArea = document.createElement('textarea')
      textArea.value = text
      document.body.appendChild(textArea)
      textArea.focus()
      textArea.select()
      try {
        document.execCommand('copy')
        this.$message.success('已复制到剪贴板')
      } catch (err) {
        this.$message.error('复制失败')
      }
      document.body.removeChild(textArea)
    },
    showMapModal() {
      this.mapModalVisible = true
    },
    onMapModalVisibleChange(visible) {
      if (visible) {
        this.$nextTick(() => {
          const container = document.getElementById('showmap-modal-container')
          if (!container) {
            return
          }
          if (this.mapInstance) {
            this.mapInstance.destroy()
            this.mapInstance = null
          }
          const [lng, lat] = this.locationStr.split(',').map(Number)
          this.mapInstance = new window.AMap.Map('showmap-modal-container', {
            center: [lng, lat],
            zoom: 15
          })
          const marker = new window.AMap.Marker({
            position: [lng, lat],
            map: this.mapInstance
          })
          // 将marker添加到实例，以便后续可能需要操作
          this.currentMarker = marker
        })
      } else if (this.mapInstance) {
        this.mapInstance.destroy()
        this.mapInstance = null
      }
    }
  },
  beforeDestroy() {
    // 清理自动刷新定时器
    if (this.refreshTimer) {
      clearInterval(this.refreshTimer)
      this.refreshTimer = null
    }
  }
}
</script>

<style scoped>
/* 页面容器样式 */
.app-container {
  background: #ffffff;
  /* padding: 16px; */
}

.ant-card {
  background: #ffffff;
  border: none;
  box-shadow: none;
}

.ant-form-item {
  margin-bottom: 4px;
}

#showmap-container {
  margin-top: 10px;
  width: 100%;
  height: calc(100% - 78px);
}

.address-view {
  margin-top: 30px;
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}

/* 页面头部样式 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #ffffff;
  border-bottom: 1px solid #e8eaed;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-btn {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  color: #64748b;
}

.back-btn:hover {
  background: #e2e8f0;
  border-color: #1966ff;
  color: #1966ff;
  transform: scale(1.05);
}

.page-title h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #262626;
  line-height: 1.2;
}

.device-id {
  margin: 8px 0 0 0;
  font-size: 14px;
  color: #595959;
  font-family: 'Courier New', monospace;
  font-weight: 500;
  background: #f8f9fa;
  padding: 6px 12px;
  border-radius: 4px;
  border: 1px solid #e8e8e8;
  display: inline-block;
  letter-spacing: 0.5px;
}

/* 自定义标签页样式 */
.custom-tabs-container {
  background: #fff;
  border-radius: 6px;
  border: 1px solid #e8e8e8;
  overflow: hidden;
}

/* 智能提示样式 */
.smart-tip {
  display: inline-flex;
  align-items: center;
  margin-left: 12px;
  padding: 4px 8px;
  background: #f0f9ff;
  border: 1px solid #bae6fd;
  border-radius: 6px;
  font-size: 12px;
  color: #0369a1;
  transition: all 0.2s ease;
  cursor: pointer;
}

.smart-tip:hover {
  background: #e0f2fe;
  border-color: #7dd3fc;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(3, 105, 161, 0.15);
}

.tip-icon {
  margin-right: 6px;
  font-size: 14px;
  color: #0284c7;
}

.tip-text {
  font-weight: 500;
  line-height: 1.4;
}

/* 不同提示类型的样式 */
.smart-tip[data-type="noData"] {
  background: #fef3c7;
  border-color: #fbbf24;
  color: #92400e;
}

.smart-tip[data-type="noData"] .tip-icon {
  color: #f59e0b;
}

.smart-tip[data-type="noData"]:hover {
  background: #fde68a;
  border-color: #f59e0b;
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.15);
}

.smart-tip[data-type="dataTypeMismatch"] {
  background: #fef2f2;
  border-color: #f87171;
  color: #991b1b;
}

.smart-tip[data-type="dataTypeMismatch"] .tip-icon {
  color: #ef4444;
}

.smart-tip[data-type="dataTypeMismatch"]:hover {
  background: #fee2e2;
  border-color: #ef4444;
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.15);
}

.smart-tip[data-type="jsonError"] {
  background: #fdf4ff;
  border-color: #e879f9;
  color: #7c2d12;
}

.smart-tip[data-type="jsonError"] .tip-icon {
  color: #d946ef;
}

.smart-tip[data-type="jsonError"]:hover {
  background: #fae8ff;
  border-color: #d946ef;
  box-shadow: 0 2px 8px rgba(217, 70, 239, 0.15);
}

.custom-tabs-nav {
  display: flex;
  background: #fafafa;
  border-bottom: 1px solid #e8e8e8;
  overflow-x: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.custom-tabs-nav::-webkit-scrollbar {
  display: none;
}

.custom-tab-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: #666;
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  min-width: fit-content;
  border-right: 1px solid #e8e8e8;
  background: #fafafa;
}

.custom-tab-item:last-child {
  border-right: none;
}

.custom-tab-item:hover {
  background: #e6f7ff;
  color: #1890ff;
}

.custom-tab-item.active {
  background: #ffffff;
  color: #1890ff;
  border-bottom: 2px solid #1890ff;
  margin-bottom: -1px;
}

.custom-tab-content {
  min-height: 500px;
  background: #fff;
}

.tab-pane {
  padding: 10px;
}

/* 设备信息页面样式 */
.device-basic-info {
  background: #ffffff;
  border-radius: 6px;
  padding: 16px;
  margin-bottom: 16px;
}

.basic-info-header {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e8e8e8;
}

.basic-info-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.basic-info-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}

.info-item {
  display: flex;
  align-items: center;
  padding: 8px 0;
}

.info-label {
  min-width: 90px;
  width: 90px;
  font-size: 13px;
  color: #8c8c8c;
  margin-right: 12px;
  flex-shrink: 0;
  text-align: left;
}

.info-value-group {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.info-value, .info-link {
  font-size: 13px;
  color: #262626;
  /* word-break: break-all; */
  line-height: 1.4;
  max-width: 420px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.copy-action-btn {
  width: 18px;
  height: 18px;
  border-radius: 3px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8c8c8c;
  background: transparent;
  border: none;
  transition: all 0.2s ease;
  font-size: 10px;
  margin-left: 2px;
  flex-shrink: 0;
}

.copy-action-btn:hover {
  color: #1890ff;
  background: #f0f8ff;
}

.tag-group {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.device-location-info {
  background: #ffffff;
  border-radius: 6px;
  padding: 16px;
  border: 1px solid #e8e8e8;
}

.location-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e8e8e8;
}

.location-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.location-coords {
  font-size: 13px;
  color: #8c8c8c;
}

.map-container {
  height: 300px;
  border-radius: 6px;
  overflow: hidden;
}

/* 简化其他页面样式 */
.simple-header {
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e8e8e8;
}

.simple-header h3 {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.simple-header p {
  margin: 0;
  font-size: 13px;
  color: #8c8c8c;
}

.action-buttons {
  margin: 16px 0;
}

.info-message {
  padding: 12px;
  background: #f6ffed;
  border: 1px solid #b7eb8f;
  border-radius: 6px;
  margin-top: 16px;
}

.info-message span {
  font-size: 13px;
  color: #52c41a;
}

.shadow-basic-info {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
  padding: 16px;
  background: #fafafa;
  border-radius: 6px;
}

.shadow-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.shadow-label {
  font-size: 13px;
  color: #8c8c8c;
}

.shadow-data-section {
  margin-top: 20px;
}

.shadow-data-section h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: #262626;
}

/* 运行状态页面样式 */
.status-container {
  padding: 0;
}

.status-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e8e8e8;
}

.status-title {
  font-size: 15px;
  font-weight: 600;
  color: #262626;
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.refresh-btn {
  background: #1890ff;
  border: none;
  border-radius: 6px;
  color: white;
  padding: 6px 12px;
  font-weight: 500;
  font-size: 12px;
  transition: all 0.2s ease;
}

.refresh-btn:hover {
  background: #40a9ff;
}

.status-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.status-card {
  background: #ffffff;
  border-radius: 8px;
  padding: 16px;
  border: 1px solid #e8e8e8;
  transition: all 0.2s ease;
  display: flex;
  flex-direction: column;
  min-height: 160px;
}

.status-card:hover {
  border-color: #1890ff;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.card-title {
  font-weight: 600;
  color: #262626;
  font-size: 13px;
  flex: 1;
  margin-right: 12px;
  word-break: break-word;
  line-height: 1.4;
}

.property-key, .event-key {
  font-size: 11px;
  color: #8c8c8c;
  font-weight: normal;
}

.card-badge {
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 10px;
  font-weight: 600;
  flex-shrink: 0;
  white-space: nowrap;
}

.badge-property {
  background: #e6f7ff;
  color: #1890ff;
  border: 1px solid #91d5ff;
}

.badge-event {
  background: #fff7e6;
  color: #fa8c16;
  border: 1px solid #ffd591;
}

.badge-warning {
  background: #fff2f0;
  color: #ff4d4f;
  border: 1px solid #ffb3b3;
}

.badge-dot {
  margin-right: 4px;
}

.card-value {
  font-size: 18px;
  font-weight: 600;
  color: #262626;
  margin: 0 0 16px 0;
  word-break: break-word;
  flex-grow: 1;
  line-height: 1.3;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60px;
}

.property-value {
  color: #1890ff;
}

.event-value {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.event-count {
  font-size: 20px;
  font-weight: 700;
  color: #fa8c16;
}

.event-description {
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.4;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.card-time {
  font-size: 11px;
  color: #8c8c8c;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 4px;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.history-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background: #1890ff;
  color: white;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
  box-shadow: 0 1px 2px rgba(24, 144, 255, 0.2);
}

.history-btn:hover {
  background: #40a9ff;
  box-shadow: 0 2px 4px rgba(24, 144, 255, 0.3);
  transform: translateY(-1px);
}

/* 轨迹按钮样式 */
.track-btn {
  background: #52c41a;
}

.track-btn:hover {
  background: #73d13d;
  box-shadow: 0 2px 4px rgba(82, 196, 26, 0.3);
  transform: translateY(-1px);
}

.history-text {
  font-size: 10px;
  line-height: 1;
  font-weight: 600;
}

.copy-btn, .detail-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #f5f5f5;
  color: #666;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 12px;
}

.copy-btn:hover, .detail-btn:hover {
  background: #1890ff;
  color: white;
}

.empty-state {
  text-align: center;
  padding: 40px 24px;
  background: #fafafa;
  border-radius: 8px;
  border: 1px dashed #d9d9d9;
}

.empty-text {
  font-size: 14px;
  color: #8c8c8c;
  font-weight: 500;
  margin-bottom: 16px;
}

.empty-action {
  margin-top: 16px;
}

.event-title {
  color: #fa8c16;
}

.property-card {
  border-left: 1px solid #b1c5d7;
}

.event-card {
  border-left: 1px solid #e3bc92;
}

.warning-message {
  background: #fff7e6;
  border: 1px solid #ffd591;
  border-radius: 6px;
  padding: 12px 16px;
  color: #d46b08;
  font-size: 13px;
  margin-top: 16px;
}

.warning-message span {
  font-size: 13px;
  color: #d46b08;
}

.info-item-full {
  grid-column: 1 / -1;
}

.info-link {
  color: #1890ff;
  transition: color 0.2s;
  cursor: pointer;
}

.info-link:hover {
  color: #40a9ff;
  text-decoration: underline;
}

.map-action-btn {
  margin-left: 6px;
  color: #1890ff;
  font-size: 16px;
  vertical-align: middle;
}

.map-action-btn:hover {
  color: #40a9ff;
  background: #f0f8ff;
}

/* 物模型标签页样式 */
.metadata-wrapper {
  background: #ffffff;
  border-top: none;
  border-radius: 0 0 12px 12px;
  overflow: hidden;
}

/* :deep 选择器需要 SCSS，暂时注释 */
/* :deep(.metadata-container) */
.metadata-container {
  background: transparent;
  min-height: auto;
  padding: 0;
}

.metadata-tabs {
  /* 需要 SCSS 的 :deep 选择器 */
}

.ant-tabs-bar {
  background: #f8fafc;
  margin: 0;
  border-radius: 0;
  border: none;
}

.tab-content {
  border: none;
  border-radius: 0;
  box-shadow: none;
}
</style>

/* 只影响InstanceDetails.vue弹窗 */
<style scoped>
.instance-event-modal-content {
  padding: 8px 0;
}

.instance-event-row {
  display: flex;
  margin-bottom: 10px;
}

.instance-event-label {
  min-width: 90px;
  color: #888;
  font-weight: 500;
}

.instance-event-value {
  color: #222;
  font-weight: 600;
  margin-left: 8px;
}
</style>
