# Patch Lifecycle 生产上线 Runbook（华为云 Linux）

> 目标：在一台有公网 IP 的华为云 Linux 服务器完成稳定上线，并具备可回滚能力。

> 若为**测试环境**且无法直连 Docker Hub，请参考：
> `docs/ops/PRIVATE_REGISTRY_TEST_DEPLOY.md`

> 若为**无域名/IP直连测试环境**并希望免配置一键安装，请执行：
> `sudo bash scripts/huawei/bootstrap-test-ip.sh`

## 0. 变量约定

- 域名：`api.example.com`
- 服务器：`ubuntu@<公网IP>`
- 部署目录：`/opt/patch-lifecycle`

## 1. 上线前检查（必做）

1. 服务器安全组放行：
   - `22/tcp`（SSH）
   - `80/tcp`（HTTP）
   - `443/tcp`（HTTPS）
2. 域名 A 记录指向服务器公网 IP
3. 代码已在目标分支且拉取到最新
4. `.env` 已修改：
   - `POSTGRES_PASSWORD` 非默认值
   - `APP_BIND_IP/DB_BIND_IP` 建议保持 `127.0.0.1`

## 2. 一键首发（推荐）

```bash
cd /opt/patch-lifecycle
cp .env.example .env
vim .env

sudo bash scripts/huawei/bootstrap-production.sh \
  --domain api.example.com \
  --email admin@example.com \
  --mode bluegreen \
  --app-dir /opt/patch-lifecycle
```

## 3. 分步执行（可替代首发）

```bash
sudo bash scripts/huawei/server-init.sh
bash scripts/huawei/preflight-check.sh api.example.com
sudo bash scripts/huawei/deploy-bluegreen.sh
sudo bash scripts/huawei/install-reverse-proxy.sh api.example.com 127.0.0.1:18080
sudo bash scripts/huawei/enable-https.sh api.example.com admin@example.com
sudo bash scripts/huawei/install-systemd-service.sh /opt/patch-lifecycle bluegreen
```

## 4. 发布后验收

```bash
bash scripts/huawei/healthcheck.sh https://api.example.com
bash scripts/huawei/post-deploy-smoke.sh https://api.example.com
```

人工验证：
- Swagger：`https://api.example.com/swagger-ui.html`
- 健康检查：`https://api.example.com/actuator/health`

## 5. 版本升级流程（蓝绿）

```bash
cd /opt/patch-lifecycle
git pull
sudo bash scripts/huawei/deploy-bluegreen.sh
```

说明：
- 自动部署到闲置槽位（blue/green）
- 健康检查通过后自动切流
- 默认保留旧槽位，便于秒级回滚

## 6. 回滚

```bash
sudo bash scripts/huawei/rollback-bluegreen.sh
```

或显式指定：

```bash
sudo bash scripts/huawei/rollback-bluegreen.sh blue
sudo bash scripts/huawei/rollback-bluegreen.sh green
```

## 7. 备份与恢复

备份：

```bash
bash scripts/huawei/db-backup.sh
```

恢复（危险操作）：

```bash
bash scripts/huawei/db-restore.sh backups/<backup-file>.sql
```

建议配置定时任务：

```bash
sudo bash scripts/huawei/install-maintenance-cron.sh /opt/patch-lifecycle
```

## 8. 监控安装与检查

安装：

```bash
bash scripts/huawei/install-monitoring.sh
sudo bash scripts/huawei/install-monitoring-systemd-service.sh /opt/patch-lifecycle
```

检查：

```bash
bash scripts/huawei/monitoring-smoke.sh
bash scripts/huawei/test-alerting.sh
```

访问：
- Prometheus: `http://127.0.0.1:9090`
- Alertmanager: `http://127.0.0.1:9093`
- Grafana: `http://127.0.0.1:3000`

告警接收配置文件：
- `ops/monitoring/alertmanager/alertmanager.yml`
- 如需接入企业微信/钉钉/邮件，请在该文件中增加对应 receiver 并重启监控栈

## 9. 常用排障命令

```bash
docker compose -f docker-compose.bluegreen.yml ps
docker compose -f docker-compose.bluegreen.yml logs -f app-blue
docker compose -f docker-compose.bluegreen.yml logs -f app-green
docker compose -f docker-compose.bluegreen.yml logs -f postgres

sudo nginx -t
sudo systemctl status nginx --no-pager
sudo systemctl status patch-lifecycle.service --no-pager
sudo systemctl status patch-lifecycle-monitoring.service --no-pager
```

## 10. 安全建议（强烈推荐）

1. PostgreSQL 保持 `127.0.0.1` 绑定，不要暴露公网
2. SSH 使用密钥登录，禁用密码登录
3. 定期执行证书续期检查：
   ```bash
   sudo bash scripts/huawei/renew-cert.sh
   ```
4. 定期备份数据库并把备份异地存储
5. 对 `.env` 做最小权限控制（`chmod 600 .env`）
6. 监控端口（9090/3000）建议仅监听本机并通过堡垒机访问

