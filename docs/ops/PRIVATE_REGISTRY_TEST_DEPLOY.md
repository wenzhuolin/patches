# 测试环境：国内私有仓库部署（无公网域名/IP直连）

> 场景：服务器无法直连 Docker Hub，希望使用国内私有仓库（ACR/TCR/SWR）并一键部署。

## 1. 准备私有仓库镜像

需要至少准备以下镜像（可在可联网机器构建/同步后推送）：

- 应用镜像：`<REGISTRY>/<NAMESPACE>/patch-lifecycle:latest`
- PostgreSQL：`<REGISTRY>/<NAMESPACE>/postgres:16-alpine`
- Java Builder 基础镜像（仅源码构建需要）：`<REGISTRY>/<NAMESPACE>/eclipse-temurin:21-jdk`
- Java Runtime 基础镜像（仅源码构建需要）：`<REGISTRY>/<NAMESPACE>/eclipse-temurin:21-jre`

## 2. 服务器登录私有仓库

```bash
docker login <REGISTRY>
```

## 3. 配置 `.env`（关键）

```bash
cp .env.example .env
vim .env
```

推荐测试环境配置：

```env
APP_BIND_IP=0.0.0.0
DB_BIND_IP=127.0.0.1
APP_PORT=8080
POSTGRES_PASSWORD=please_change_me

APP_IMAGE=<REGISTRY>/<NAMESPACE>/patch-lifecycle:latest
POSTGRES_IMAGE=<REGISTRY>/<NAMESPACE>/postgres:16-alpine
JAVA_BUILDER_IMAGE=<REGISTRY>/<NAMESPACE>/eclipse-temurin:21-jdk
JAVA_RUNTIME_IMAGE=<REGISTRY>/<NAMESPACE>/eclipse-temurin:21-jre

# 私有仓库优先：拉镜像，不本地构建，不强制外网pull
APP_BUILD_ENABLED=false
APP_PULL_IMAGE=true
COMPOSE_BUILD_PULL=false
```

## 4. 一键部署（标准模式）

```bash
sudo bash scripts/huawei/bootstrap-production.sh \
  --mode prod \
  --app-dir /opt/patch-lifecycle \
  --with-monitoring false \
  --with-maintenance-cron false
```

> 不传 `--domain` 会自动跳过 Nginx/HTTPS，适合 IP 直连测试环境。

## 5. 验证

```bash
bash scripts/huawei/healthcheck.sh http://127.0.0.1:8080
curl -f http://<服务器IP>:8080/actuator/health
```

Swagger:

```text
http://<服务器IP>:8080/swagger-ui.html
```

## 6. 常见问题

### Q1: `resolve image config timeout`

表示仓库网络不可达。请检查：

1. `docker login <REGISTRY>` 是否成功
2. 私有仓库地址是否可达（企业防火墙/白名单）
3. `APP_PULL_IMAGE=true` 时镜像 tag 是否存在

### Q2: 仍触发源码构建

确认 `.env` 中：

```env
APP_BUILD_ENABLED=false
```

若已配置，执行：

```bash
sudo systemctl restart patch-lifecycle.service
```
