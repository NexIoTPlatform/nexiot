#!/bin/bash

#======================================================================
# 启动/重启脚本公共方法
# 负责解析 active profile、logging.file.path 和日志查看提示
#======================================================================

# 项目名称
APPLICATION="cn-universal-web"

# 项目启动jar包名称
APPLICATION_JAR="${APPLICATION}.jar"

# 主类名
MAIN_CLASS="cn.universal.CnUniversalIoTApplication"

# bin目录绝对路径
BIN_PATH=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)

# 项目根目录绝对路径
BASE_PATH=$(cd "${BIN_PATH}/.." && pwd)

# 外部配置文件绝对目录
CONFIG_DIR="${BASE_PATH}/config"

# 脚本运行日志目录
RUNTIME_LOG_DIR="$(dirname "${BASE_PATH}")/logs"

# 启动日志文件
STARTUP_LOG_PATH="${RUNTIME_LOG_DIR}/startup.log"

LOG_UTILS_ENABLED=1

trim_value() {
  local value="${1-}"
  value="${value#"${value%%[![:space:]]*}"}"
  value="${value%"${value##*[![:space:]]}"}"
  printf '%s' "$value"
}

read_property_value() {
  local file_path="$1"
  local key="$2"

  [ -f "$file_path" ] || return 1

  awk -v key="$key" '
    BEGIN {
      pattern = "^[[:space:]]*" key "[[:space:]]*[:=][[:space:]]*"
      found = 0
    }
    /^[[:space:]]*[#!]/ { next }
    $0 ~ pattern {
      value = $0
      sub(pattern, "", value)
      result = value
      found = 1
    }
    END {
      if (found) {
        sub(/[[:space:]]+$/, "", result)
        print result
      } else {
        exit 1
      }
    }
  ' "$file_path" | tr -d '\r'
}

resolve_env_placeholders() {
  local value="${1-}"
  local resolved="$value"
  local replace_count=0
  local placeholder_pattern='\$\{([A-Za-z_][A-Za-z0-9_]*)(:-|:)?([^}]*)\}'

  while [[ "$resolved" =~ $placeholder_pattern ]] && [ $replace_count -lt 10 ]; do
    local whole_match="${BASH_REMATCH[0]}"
    local var_name="${BASH_REMATCH[1]}"
    local default_mode="${BASH_REMATCH[2]}"
    local default_value="${BASH_REMATCH[3]}"
    local env_value="${!var_name-}"
    local replacement=""

    if [ -n "$env_value" ]; then
      replacement="$env_value"
    elif [ -n "$default_mode" ]; then
      replacement="$default_value"
    fi

    resolved="${resolved//$whole_match/$replacement}"
    ((replace_count++))
  done

  printf '%s' "$resolved"
}

is_unresolved_build_placeholder() {
  local value="${1-}"
  [[ "$value" == @*@ ]]
}

guess_single_active_profile() {
  local count=0
  local guessed_profile=""
  local file_path

  for file_path in "${CONFIG_DIR}"/application-*.properties; do
    [ -f "$file_path" ] || continue

    guessed_profile="${file_path##*/}"
    guessed_profile="${guessed_profile#application-}"
    guessed_profile="${guessed_profile%.properties}"
    ((count++))
  done

  if [ "$count" -eq 1 ]; then
    printf '%s' "$guessed_profile"
  fi
}

get_active_profiles() {
  local profiles="${SPRING_PROFILES_ACTIVE-}"

  if [ -z "$profiles" ]; then
    profiles=$(read_property_value "${CONFIG_DIR}/application.properties" "spring.profiles.active" 2>/dev/null || true)
  fi

  profiles=$(trim_value "$(resolve_env_placeholders "$profiles")")

  if [ -z "$profiles" ] || is_unresolved_build_placeholder "$profiles"; then
    profiles=$(guess_single_active_profile)
  fi

  printf '%s' "$profiles"
}

get_configured_logging_file_path() {
  local configured_path=""
  local candidate=""
  local profiles=""
  local profile=""
  local profile_file=""

  candidate=$(read_property_value "${CONFIG_DIR}/application.properties" "logging.file.path" 2>/dev/null || true)
  if [ -n "$candidate" ]; then
    configured_path="$candidate"
  fi

  profiles=$(get_active_profiles)
  if [ -n "$profiles" ]; then
    IFS=',' read -r -a profile_list <<< "$profiles"
    for profile in "${profile_list[@]}"; do
      profile=$(trim_value "$profile")
      [ -n "$profile" ] || continue

      profile_file="${CONFIG_DIR}/application-${profile}.properties"
      candidate=$(read_property_value "$profile_file" "logging.file.path" 2>/dev/null || true)
      if [ -n "$candidate" ]; then
        configured_path="$candidate"
      fi
    done
  fi

  if [ -z "$configured_path" ] && [ -n "${LOGGING_FILE_PATH-}" ]; then
    configured_path="${LOGGING_FILE_PATH}"
  fi

  configured_path=$(trim_value "$(resolve_env_placeholders "$configured_path")")
  printf '%s' "$configured_path"
}

normalize_path_if_possible() {
  local target_path="${1-}"
  local parent_dir=""
  local base_name=""

  target_path="${target_path%/}"
  [ -n "$target_path" ] || return 0

  if [ -d "$target_path" ]; then
    (cd "$target_path" && pwd -P)
    return 0
  fi

  parent_dir=$(dirname "$target_path")
  base_name=$(basename "$target_path")

  if [ -d "$parent_dir" ]; then
    printf '%s/%s' "$(cd "$parent_dir" && pwd -P)" "$base_name"
    return 0
  fi

  printf '%s' "$target_path"
}

get_application_log_dir() {
  local configured_path=""
  local resolved_path=""

  configured_path=$(get_configured_logging_file_path)
  if [ -z "$configured_path" ]; then
    configured_path="./logs"
  fi

  case "$configured_path" in
    ~/*)
      resolved_path="${HOME}/${configured_path#~/}"
      ;;
    /*)
      resolved_path="$configured_path"
      ;;
    *)
      resolved_path="${BASE_PATH}/${configured_path}"
      ;;
  esac

  normalize_path_if_possible "$resolved_path"
}

print_log_help() {
  local app_log_dir="${1:-$(get_application_log_dir)}"
  local app_all_log="${app_log_dir}/all.log"
  local app_error_log="${app_log_dir}/error.log"

  echo "如需持续查看日志，可直接复制执行:"
  echo "tail -n 200 -f \"${app_all_log}\""
  echo "tail -n 200 -f \"${app_error_log}\""
  echo "tail -n 200 -f \"${STARTUP_LOG_PATH}\""
}

print_recent_log_preview() {
  local app_log_dir="${1:-$(get_application_log_dir)}"
  local app_all_log="${app_log_dir}/all.log"

  if [ -s "$app_all_log" ]; then
    echo "Recent application logs (last 20 lines):"
    tail -n 20 "$app_all_log" || true
  elif [ -f "$app_all_log" ]; then
    echo "Application log file exists but is empty for now: ${app_all_log}"
    echo "The process may still be initializing. You can keep watching it with:"
    echo "tail -n 200 -f \"${app_all_log}\""
  else
    echo "Application log file has not been created yet: ${app_all_log}"
    echo "The process may still be initializing. You can keep watching it with:"
    echo "tail -n 200 -f \"${app_all_log}\""
  fi
}
