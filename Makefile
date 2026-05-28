.PHONY: build clean up-clean up down
GREEN  := \033[32m
YELLOW := \033[33m

down:
	docker compose down --remove-orphans

clean:
	@$(MAKE) down
	./gradlew clean build -x test

up-clean: clean up

build:
	@echo "$(GREEN)Building Project..."
	./gradlew clean build -x test

up:
	@echo "$(GREEN)Starting Project..."
	@$(MAKE) build
	docker compose -f docker-compose.yml up -d --remove-orphans --build

status:
	@echo "$(YELLOW)Containers Status"
	@docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"