FROM registry.access.redhat.com/rhscl/httpd-24-rhel7:latest
ARG URL
ENV URL ${URL:-unset}
ARG REPO
ENV REPO ${REPO:-https://github.com/openshift-integration/camel-k-example-event-streaming}
ARG BRANCH
ENV BRANCH ${BRANCH:-1.6.x}
RUN mkdir tmp && \
	curl --output project.zip -LOk ${REPO}/archive/${BRANCH}.zip && \
	unzip project.zip && \
	sed "s#changeme#$URL#" -i camel-k-example-event-streaming-${BRANCH}/front-end/src/main/resources/site/js/index.js && \
	mv camel-k-example-event-streaming-${BRANCH}/front-end/src/main/resources/site/* ${HTTPD_DATA_PATH}/html && \
	rm -rf project.zip camel-k-example-event-streaming-${BRANCH}
