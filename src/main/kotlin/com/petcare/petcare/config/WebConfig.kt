package com.petcare.petcare.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig : WebMvcConfigurer {
    // ngrok을 통한 HTTPS 접속을 위한 추가 설정이 필요하면 여기에 추가
}



