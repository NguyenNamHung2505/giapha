# Build stage
FROM node:18-alpine AS build
WORKDIR /app

# Copy package files first for caching
COPY package*.json ./
RUN npm ci

# Copy source and build
COPY . .
RUN npm run build -- --configuration production

# Runtime stage - Nginx
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Remove default nginx static assets
RUN rm -rf ./*

# Copy built Angular app
COPY --from=build /app/dist/frontend/browser .

# Copy nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
