# Puerta de Enlace Centralizada (api-gateway)

Microservicio desarrollado sobre **Spring Cloud Gateway** que actúa como el punto de entrada único (Single Point of Entry) de la arquitectura para todas las aplicaciones clientes.

## Funcionalidades Mínimas Implementadas
*   **Punto de Entrada Único:** Centraliza la recepción de peticiones provenientes de Postman, aplicaciones web frontend o dispositivos móviles.
*   **Cliente de Descubrimiento:** Registro automático como cliente activo dentro del servidor `eureka-server`.
*   **Enrutamiento Dinámico (Dynamic Routing):** Balanceo de carga y redirección hacia los microservicios destino utilizando los nombres lógicos registrados en Eureka:
    *   `lb://SHIPMENT-SERVICE`
    *   `lb://COURIER-SERVICE`
    *   `lb://SECURITY-SERVICE`
*   **Gestión Centralizada de Seguridad:**
    *   Implementación de filtros globales (`GatewayFilters`) para interceptar y validar el token JWT enviado en la cabecera `Authorization: Bearer <token>`.
    *   Delegación del proceso de autenticación inicial al componente `security-service`.
    *   Protección estricta de rutas hacia `shipment-service` y `courier-service`, restringiendo el acceso según los roles extraídos del JWT (`ADMIN`, `CLIENTE`, `REPARTIDOR`).

## Configuración Principal (`application.yml`)
```yaml
server:
  port: 8080

spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
      routes:
        - id: security-service
          uri: lb://SECURITY-SERVICE
          predicates:
            - Path=/api/v1/auth/**
            
        - id: shipment-service
          uri: lb://SHIPMENT-SERVICE
          predicates:
            - Path=/api/v1/shipments/**
            
        - id: courier-service
          uri: lb://COURIER-SERVICE
          predicates:
            - Path=/api/v1/couriers/**

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8066/eureka/