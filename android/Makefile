.PHONY: sync clean build
.DEFAULT_GOAL := build
SDK_VERSION=4.0.6

SINCH_SDK_DIR = ./app/libs
SINCH_SDK_BINARY = ${SINCH_SDK_DIR}/sinch-android-rtc-${SDK_VERSION}.zip
SINCH_SDK_AAR = ${SINCH_SDK_DIR}/sinch-android-rtc-${SDK_VERSION}.aar


clean:
	rm -rf ${SINCH_SDK_DIR}
	./gradlew clean

sync: |${SINCH_SDK_AAR}

${SINCH_SDK_AAR}: ${SINCH_SDK_BINARY}
	mv ${SINCH_SDK_DIR}/sinch-android-rtc-${SDK_VERSION}/libs/*.aar ${SINCH_SDK_DIR}
	rm -rf ${SINCH_SDK_DIR}/sinch-android-rtc-${SDK_VERSION}/
	rm -rf ${SINCH_SDK_DIR}/*.zip
	touch $@

${SINCH_SDK_BINARY}: ${SINCH_SDK_DIR}
	curl -0 "http://artifactory:8081/artifactory/ext-release-local/sinch_releases/sinch_android_rtc_${SDK_VERSION}/sinch-android-rtc-${SDK_VERSION}.zip" -o ${SINCH_SDK_BINARY}
	cd ${SINCH_SDK_DIR} && unzip -o ./$(notdir $(SINCH_SDK_BINARY))	

${SINCH_SDK_DIR}:
	mkdir -p $@
