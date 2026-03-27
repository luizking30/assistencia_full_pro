# 🛠️ Sistema de Gestão de Assistência Técnica (Full-Stack)

[![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Thymeleaf](https://img.shields.io/badge/Thymeleaf-005F0F?style=for-the-badge&logo=thymeleaf&logoColor=white)](https://www.thymeleaf.org/)

## 📖 Sobre o Projeto
Este sistema foi desenvolvido para profissionalizar e automatizar a gestão de uma assistência técnica de eletrônicos (**Shark Eletrônicos**). Ele nasceu da necessidade real de controlar o fluxo de entrada e saída de aparelhos, garantindo que o histórico de cada cliente e o estoque de peças estivessem sempre organizados.

O projeto utiliza a arquitetura **MVC (Model-View-Controller)**, garantindo uma separação clara entre a lógica de negócio, a persistência de dados e a interface do usuário.

## 🚀 Funcionalidades Principais

* **Gestão de Clientes:** Cadastro completo com histórico de serviços realizados.
* **Ordens de Serviço (OS):** Emissão de OS com descrição de defeitos, laudo técnico, status (Em Orçamento, Aprovado, Concluído) e valores.
* **Controle de Estoque:** Cadastro de peças e produtos com atualização em tempo real.
* **Dashboard Administrativo:** Painel visual com métricas de faturamento e serviços pendentes.
* **Segurança:** Implementação de regras de negócio para integridade dos dados.

## 🛠️ Tecnologias e Ferramentas

### **Back-end**
* **Java 17+**: Linguagem base do sistema.
* **Spring Boot**: Framework principal para agilidade no desenvolvimento.
* **Spring Data JPA**: Abstração para persistência de dados.
* **Hibernate**: Gerenciamento de mapeamento objeto-relacional (ORM).
* **MySQL**: Banco de dados relacional para armazenamento seguro.

### **Front-end**
* **Thymeleaf**: Engine para renderização de páginas dinâmicas no servidor.
* **Bootstrap 5**: Design responsivo e componentes modernos.
* **JavaScript/HTML5/CSS3**: Personalizações de interface e comportamento.

### **Ferramentas de Desenvolvimento**
* **Maven**: Gestão de dependências e build do projeto.
* **Git & GitHub**: Controle de versão.

## 💻 Como Executar o Projeto

1.  **Clone o repositório:**
    ```bash
    git clone [https://github.com/luizking30/assistencia_full_pro.git](https://github.com/luizking30/assistencia_full_pro.git)
    ```

2.  **Configuração do Banco de Dados:**
    Certifique-se de ter o MySQL instalado e crie um schema. No arquivo `src/main/resources/application.properties`, configure:
    ```properties
    spring.datasource.url=jdbc:mysql://localhost:3306/nome_do_seu_banco
    spring.datasource.username=seu_usuario
    spring.datasource.password=sua_senha
    ```

3.  **Build e Run:**
    ```bash
    mvn clean install
    mvn spring-boot:run
    ```
    A aplicação estará disponível em `http://localhost:8080`.

---

## 👨‍💻 Sobre o Desenvolvedor

**Luiz Eduardo Mendonça Amorim**
* 🎓 Estudante de **Sistemas de Informação** (4º Semestre).
* 🛠️ 10 anos de experiência como Técnico e Empreendedor na área de eletrônicos.
* 🚀 Buscando estágio em desenvolvimento **Java** ou **Python**.

**Contatos:**
* 📱 WhatsApp: (61) 9 8104-8509
* 📧 E-mail: luiz.eduardo.amorim@hotmail.com
* 📍 Taguatinga, DF - Brasília.

---
*Este projeto é parte do meu portfólio profissional e está em constante evolução.*
