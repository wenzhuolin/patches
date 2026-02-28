# 无域名/IP直连测试环境：免配置一键安装

> 目标：无需私有仓库、无需手改 `.env`，直接使用国内镜像加速完成安装。

## 一键安装命令

```bash
cd /opt/patch-lifecycle
sudo bash scripts/huawei/bootstrap-test-ip.sh
```

脚本行为：

1. 若不存在 `.env`，自动从 `.env.example` 生成  
2. 自动生成 `POSTGRES_PASSWORD`、`GRAFANA_ADMIN_PASSWORD`（避免默认弱口令）  
3. 自动设置：
   - `APP_BIND_IP=0.0.0.0`（支持 IP 直连）
   - `DB_BIND_IP=127.0.0.1`（数据库不暴露公网）
4. 自动配置 Docker 镜像加速（默认，可关闭）：
   - `https://docker.m.daocloud.io`
   - `https://mirror.ccs.tencentyun.com`
   - `https://hub-mirror.c.163.com`
5. 跳过监控和定时维护（更适合测试环境）
6. 若提供华为云 SWR 专属加速地址，会自动置于 Docker mirrors 第一优先级

## 安装后访问

- Swagger: `http://<服务器IP>:8080/swagger-ui.html`
- Health: `http://<服务器IP>:8080/actuator/health`

## 可选参数

```bash
sudo bash scripts/huawei/bootstrap-test-ip.sh \
  --app-port 8080 \
  --docker-mirrors "https://docker.m.daocloud.io,https://mirror.ccs.tencentyun.com"
```

禁用脚本写入 `/etc/docker/daemon.json`：

```bash
sudo bash scripts/huawei/bootstrap-test-ip.sh --disable-docker-mirror
```

华为云 SWR 专属加速（推荐）：

```bash
sudo bash scripts/huawei/bootstrap-test-ip.sh \
  --swr-mirror "https://<你的SWR专属加速地址>.mirror.swr.myhuaweicloud.com"
```

> SWR 专属加速地址可在华为云 SWR 控制台“镜像加速器”页面获取。
