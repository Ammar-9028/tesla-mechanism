# Tesla Roadster "Flying Car" Explorer
Java 21 + Servlet 4 (Tomcat 9) + Maven. Frontend is plain HTML/CSS/JS (no CDN, works offline).

Run locally:  mvn clean package && cp target/ROOT.war $CATALINA_HOME/webapps/   -> http://localhost:8080/
Docker:       docker build -t flyingcar . && docker run -p 8080:8080 flyingcar
Cloud:        push the Dockerfile to Render / Railway / Fly.io / Google Cloud Run / AWS App Runner (port 8080).
              Or upload target/ROOT.war to AWS Elastic Beanstalk (Tomcat 9 / Corretto 21 platform).
API:          GET /api/physics?mass=1800&gas=40&pbar=300&twr=1.5   |   GET /api/compare
