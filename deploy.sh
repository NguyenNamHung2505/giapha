#!/bin/bash

# Deployment script for Family Tree Manager
# Target: giapha.colabacademy.vn (20.212.249.209)

set -e

echo "=========================================="
echo "  Family Tree Manager - Deployment Script"
echo "=========================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo -e "${RED}Docker is not installed. Installing...${NC}"
    curl -fsSL https://get.docker.com -o get-docker.sh
    sudo sh get-docker.sh
    sudo usermod -aG docker $USER
    rm get-docker.sh
    echo -e "${GREEN}Docker installed successfully!${NC}"
    echo -e "${YELLOW}Please log out and log back in, then run this script again.${NC}"
    exit 0
fi

# Check if docker compose is available
if ! docker compose version &> /dev/null; then
    echo -e "${RED}Docker Compose plugin not found. Installing...${NC}"
    sudo apt-get update
    sudo apt-get install -y docker-compose-plugin
fi

# Create .env file if it doesn't exist
if [ ! -f .env ]; then
    echo -e "${YELLOW}Creating .env file from template...${NC}"
    cp .env.example .env
    echo -e "${GREEN}.env file created. Please review and update if needed.${NC}"
fi

# Stop existing containers
echo -e "${YELLOW}Stopping existing containers...${NC}"
docker compose -f docker-compose.prod.yml down 2>/dev/null || true

# Pull latest images
echo -e "${YELLOW}Pulling latest base images...${NC}"
docker compose -f docker-compose.prod.yml pull

# Build and start containers
echo -e "${YELLOW}Building and starting containers...${NC}"
docker compose -f docker-compose.prod.yml up -d --build

# Wait for services to be healthy
echo -e "${YELLOW}Waiting for services to start...${NC}"
sleep 30

# Check container status
echo ""
echo -e "${GREEN}=========================================="
echo "  Container Status"
echo "==========================================${NC}"
docker compose -f docker-compose.prod.yml ps

# Health check
echo ""
echo -e "${YELLOW}Checking backend health...${NC}"
for i in {1..10}; do
    if curl -s http://localhost:8080/actuator/health | grep -q "UP"; then
        echo -e "${GREEN}Backend is healthy!${NC}"
        break
    fi
    echo "Waiting for backend... ($i/10)"
    sleep 5
done

echo ""
echo -e "${GREEN}=========================================="
echo "  Deployment Complete!"
echo "==========================================${NC}"
echo ""
echo "Access your application at:"
echo "  - Website: http://giapha.colabacademy.vn"
echo "  - API: http://giapha.colabacademy.vn/api"
echo ""
echo "Useful commands:"
echo "  - View logs: docker compose -f docker-compose.prod.yml logs -f"
echo "  - Restart: docker compose -f docker-compose.prod.yml restart"
echo "  - Stop: docker compose -f docker-compose.prod.yml down"
echo ""
