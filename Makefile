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
	docker-compose -f docker-compose.local.yaml up -d --build

develop:
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

ank_account && protoc --go_out=. --go-grpc_opt=require_unimplemented_servers=false --go-grpc_out=. bank_account.proto