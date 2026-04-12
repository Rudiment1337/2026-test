#!/bin/bash
#set -x
export LC_ALL=C
export LANG=C
API_URL="http://192.168.128.200:8080/api/sensors/batch"
BUS_ID="${1:-1}"		# конструкция для выполнеия по дефолту ${$1:-default}(обрабат пустые строки(если без - то пустая строка будет считься $1))
COUNT_VALUE=1
AUTH_USER="admin"
AUTH_PASS="13371337"

gen_data() {
    engine_temp=$(echo "scale=1; 70 + ($RANDOM % 300)/10" | bc) # temperature
    tire_pressure=$(echo "scale=2; 2.2 + ($RANDOM % 50)/100" | bc) # давление
    fuel_level=$(echo "scale=1; 40 + ($RANDOM % 600)/10" | bc) # уров топлива
    (( $(echo "$fuel_level > 99" | bc -l) )) && fuel_level=99 # ((...)) раб с цифрами(если ((1)) && operat, то выполнится 1 операция, если 0 то вторая после &&
    speed=$((RANDOM % 110))	#					echo $(($(echo "44 > f" | bc -l))) = 1
    echo "$engine_temp|$tire_pressure|$fuel_level|$speed"
}

# Отправка с проверкой
send_data() {
    local json_data="$1"	# локальная переменная с 1 аргументом функции
    local response=$(curl -s -w "%{http_code}" -X POST "$API_URL" \
		-H "Content-Type: application/json" \
		-u "$AUTH_USER:$AUTH_PASS" \
		-d "$json_data" --max-time 5)	# POST с данными
    local http_code=${response: -3}	# подстрока послед 3 символа переменной
    if [[ $http_code =~ ^20[0-9]$ ]]; then # регулярка с 0-9 постав к 20(201;202..209) идёт сравнение кода http
        return 0
    else
        return 1
    fi
}

# Основной while
while true; do
	if [ $COUNT_VALUE -eq 5 ]; then
		echo "Прошло время ожидания, повторная попытка после 10 сек."
		COUNT_VALUE=1
		sleep 10
	fi
	DATA_LINE=$(gen_data)
	IFS='|' read -r engine_temp tire_pressure fuel_level speed <<< "$DATA_LINE"
	json=$(printf '{"busId":%d,"timestamp":"%s","sensors":[
	{"type":"engine_temp","value":%.1f},
	{"type":"tire_pressure","value":%.2f},
	{"type":"fuel_level","value":%.1f},
	{"type":"speed","value":%d}
	]}' "$BUS_ID" "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$engine_temp" "$tire_pressure" "$fuel_level" "$speed")

	if send_data "$json"; then
		echo "[$(date '+%H:%M:%S')] $engine_temp°C | $tire_pressure bar | $fuel_level% | $speed km/h"
		COUNT_VALUE=1
	else
	        echo "Сервер недоступен, попытка повторной передачи - $COUNT_VALUE."
		((COUNT_VALUE++))
		sleep 5
	fi
	sleep $BUS_ID
done
