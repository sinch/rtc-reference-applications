.PHONY: sync clean build
.DEFAULT_GOAL := build
SDK_VERSION=6.10.22
SDK_REVISION=86831d43

SINCH_SDK_DIR = ./app/libs
SINCH_SDK_BINARY = ${SINCH_SDK_DIR}/sinch-android-rtc-${SDK_VERSION}.zip
SINCH_SDK_AAR = ${SINCH_SDK_DIR}/sinch-android-rtc-${SDK_VERSION}.aar

clean:
	rm -rf ${SINCH_SDK_DIR}
	./gradlew clean

sync: |${SINCH_SDK_AAR}

${SINCH_SDK_AAR}: ${SINCH_SDK_BINARY}
	mv ${SINCH_SDK_DIR}/sinch-android-rtc-${SDK_VERSION}+${SDK_REVISION}/libs/*.aar ${SINCH_SDK_DIR}
	rm -rf ${SINCH_SDK_DIR}/sinch-android-rtc-${SDK_VERSION}+${SDK_REVISION}/
	rm -rf ${SINCH_SDK_DIR}/*.zip

${SINCH_SDK_BINARY}: ${SINCH_SDK_DIR}
	find ${SINCH_SDK_DIR} -name '*sinch-android-rtc*' -delete
	curl -0 "https://download.sinch.com/android/${SDK_VERSION}/sinch-android-rtc-${SDK_VERSION}%2B${SDK_REVISION}.zip" -o ${SINCH_SDK_BINARY}
	cd ${SINCH_SDK_DIR} && unzip -o ./$(notdir $(SINCH_SDK_BINARY))	

${SINCH_SDK_DIR}:
	mkdir -p $@