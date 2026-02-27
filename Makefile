.PHONY: test build run docker-build deploy-prod deploy-bg monitoring-up monitoring-smoke preflight smoke

test:
	./mvnw test

build:
	./mvnw -DskipTests clean package

run:
	./mvnw spring-boot:run

docker-build:
	docker compose -f docker-compose.prod.yml build --pull app

deploy-prod:
	bash scripts/huawei/deploy.sh

deploy-bg:
	sudo bash scripts/huawei/deploy-bluegreen.sh

monitoring-up:
	bash scripts/huawei/install-monitoring.sh

monitoring-smoke:
	bash scripts/huawei/monitoring-smoke.sh

preflight:
	bash scripts/huawei/preflight-check.sh

smoke:
	bash scripts/huawei/post-deploy-smoke.sh
