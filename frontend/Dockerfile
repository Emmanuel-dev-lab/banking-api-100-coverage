# --- Etape 1 : build du bundle statique ---
FROM node:22-alpine AS build
WORKDIR /app
# Cache des dependances : on copie d'abord les manifestes.
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# --- Etape 2 : service statique + reverse-proxy /api ---
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
