FROM nginx:alpine
COPY 00-rate-limit.conf /etc/nginx/conf.d/00-rate-limit.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf
STOPSIGNAL SIGQUIT
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -q --spider --tries=1 http://localhost/health || exit 1
