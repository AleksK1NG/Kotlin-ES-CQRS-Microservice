.PHONY:

# ==============================================================================
# Docker

local:
	@echo Clearing kafka data
	rm -rf ./es-data01
	@echo Clearing kafka data
	rm -rf ./kafka_data
	@echo Clearing zookeeper data
	rm -rf ./zookeeper
	@echo Clearing prometheus data
	rm -rf ./prometheus
	@echo Starting local docker compose
	@echo Clearing mongo data
	rm -rf ./mongodb_data_container
	docker-compose -f docker-compose.local.yaml up -d --build

develop:
	mvn clean package -Dmaven.test.skip
	@echo Clearing kafka data
	rm -rf ./es-data01
	@echo Clearing kafka data
	rm -rf ./kafka_data
	@echo Clearing zookeeper data
	rm -rf ./zookeeper
	@echo Clearing prometheus data
	rm -rf ./prometheus
	@echo Starting local docker compose
	docker-compose -f docker-compose.yaml up -d --build


# ==============================================================================
# Docker support

FILES := $(shell docker ps -aq)

down-local:
	docker stop $(FILES)
	docker rm $(FILES)

clean:
	docker system prune -f

logs-local:
	docker logs -f $(FILES)

