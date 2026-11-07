1833 curl -sSL https://raw.githubusercontent.com/voidint/g/master/install.sh | bash
1834 ls
1835 g install 1.24.1 --mirror=https://golang.google.cn/dl/
1836 source "/home/abc123/.g/env"
1837 g install 1.24.1 --mirror=https://golang.google.cn/dl/
1838 g use 1.24.1 # 切换版本
1839 ll
1840 go build -tags "with_extend,with_ai,with_ci,with_iot,with_etl,use_fasthttp" .
1841 go env -w GOPROXY=https://goproxy.cn,direct
1842 go env -w GOSUMDB=off
1843 go env | grep -E "GOPROXY|GOSUMDB"
1844 go build -tags "with_extend,with_ai,with_ci,with_iot,with_etl,use_fasthttp" .
1845 nohup ./server -c="./config.conf" >> console.log &
1846 ls
1847 nohup ./server -c="./config.conf" >> console.log &