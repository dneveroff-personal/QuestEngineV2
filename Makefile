.PHONY: build clean up-clean up down run-local
GREEN  := \033[32m
YELLOW := \033[33m

down:
	docker compose -f docker-compose.local.yml down --remove-orphans

clean:
	@echo "$(YELLOW)Полная очистка..."
	./gradlew clean
	rm -rf */build/ .gradle/ build/
	docker compose -f docker-compose.local.yml down -v --remove-orphans
	docker rmi $$(docker images "questenginev2/*:dev" -q) 2>/dev/null || true
	@echo "$(GREEN)Очистка завершена!$(RESET)"

up-clean: clean up

build:
	@echo "$(GREEN)Building Project..."
	./gradlew clean build -x test

tests:
	@echo "$(GREEN)Building Project And Testing ..."
	./gradlew clean build

up:
	@echo "$(GREEN)Starting Project..."
	@$(MAKE) build
	docker compose -f docker-compose.local.yml up -d --remove-orphans --build

status:
	@echo "$(YELLOW)Containers Status"
	@docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}\t{{.Size}}"

## SHow last 100 rows of set CONTAINER (C)
logs:
	docker logs $(C) --tail=100

rebuild-app:
	@echo "$(GREEN)Rebuilding and ReStarting Project..."
	docker compose -f docker-compose.local.yml stop app
	@$(MAKE) build
	docker compose -f docker-compose.local.yml up -d app --remove-orphans --build

# =========================================================
# Деплой
# make deploy-ghcr HOST=root@0.0.0.0 VERSION=1.0.0
# =========================================================
#deploy-ghcr:
#	@echo "Start deploy ..."
#	@if [ -z "$(HOST)" ]; then echo "Usage: make deploy-ghcr HOST=user@server VERSION=1.0.0"; exit 1; fi
#	ssh $(HOST) "\
#		cd ~/ts-wc-scores && \
#		docker compose -f docker-compose.prod.yml pull && \
#		docker compose -f docker-compose.prod.yml up -d && \
#		docker image prune -f"
#	@echo "✅ Deploy DONE"