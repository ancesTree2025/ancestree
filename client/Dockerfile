FROM oven/bun:1.1.39-alpine AS build
WORKDIR /app
COPY package.json bun.lockb ./
RUN bun install --frozen-lockfile
COPY . .
RUN bun run check
RUN bun run build

FROM oven/bun:1.1.39-alpine AS runtime
COPY --from=build /app/build ./prod
CMD ["bun", "run", "prod/index.js"]