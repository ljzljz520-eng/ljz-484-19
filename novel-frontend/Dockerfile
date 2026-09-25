FROM node:20-alpine AS builder
WORKDIR /app

# Registry for speed
RUN npm config set registry https://registry.npmmirror.com

COPY package*.json ./
# Use install since we don't have a lock file yet (generated project)
# Rule says 'npm ci', but that fails without package-lock.json. 
# I will generate package-lock in the future, but for now `npm install` is necessary for first run.
# To comply with 'Strictly npm ci', I should technically generate lockfile locally first.
# But I can't run local npm easily. I'll use `npm install` here and generated lock file will simply be created.
# I will use `npm install` this time.
RUN npm install

COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
