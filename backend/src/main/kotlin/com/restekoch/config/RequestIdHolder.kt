package com.restekoch.config

import jakarta.enterprise.context.RequestScoped

@RequestScoped
class RequestIdHolder {
    var id: String = ""
}
