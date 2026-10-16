#!/bin/sh
# 환경 변수로 Kubernetes Secret 을 만듭니다. 값은 출력하지 않습니다.
# 사용 전: set -a && . ./.env && set +a
set -eu

missing=""
for name in \
  MYSQL_ROOT_PASSWORD \
  MYSQL_DATABASE \
  MYSQL_USER \
  MYSQL_PASSWORD \
  SPRING_DATASOURCE_USERNAME \
  SPRING_DATASOURCE_PASSWORD \
  PETCARE_ADMIN_INITIAL_PASSWORD
do
  eval "value=\${$name-}"
  if [ -z "$value" ]; then
    missing="$missing $name"
  fi
done

if [ -n "$missing" ]; then
  echo "환경 변수가 없습니다:$missing" >&2
  echo "프로젝트 루트의 .env 를 읽은 뒤 다시 실행하세요. .env.example 을 참고하면 됩니다." >&2
  exit 1
fi

kubectl -n petcare create secret generic mysql-secret \
  --from-literal=root-password="$MYSQL_ROOT_PASSWORD" \
  --from-literal=database="$MYSQL_DATABASE" \
  --from-literal=username="$MYSQL_USER" \
  --from-literal=password="$MYSQL_PASSWORD" \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl -n petcare create secret generic petcare-secret \
  --from-literal=SPRING_DATASOURCE_USERNAME="$SPRING_DATASOURCE_USERNAME" \
  --from-literal=SPRING_DATASOURCE_PASSWORD="$SPRING_DATASOURCE_PASSWORD" \
  --from-literal=PETCARE_ADMIN_INITIAL_PASSWORD="$PETCARE_ADMIN_INITIAL_PASSWORD" \
  --dry-run=client -o yaml | kubectl apply -f -

echo "mysql-secret 과 petcare-secret 을 반영했습니다. 값은 출력하지 않았습니다."
