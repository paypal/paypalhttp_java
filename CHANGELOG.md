## unreleased
* Change JSON serializer to use gson.
* Replace error JsonParseException to MalformedJsonException as a breaking change.

## 1.3.2
* Fix Case Sensitivity of Content Type for deserialization process

## 1.3.1
* Fix merge of branches for code.

## 1.3.0
* Add multipart/form-data support with JSON parts.

## 1.2.19
* Fix deserialization of null values in json list.

## 1.2.18
* Enable deserialization of raw list responses in JSON.

## 1.2.17
* Ensure error responses aren't umarshaled.

## 1.2.16
* Fix urlencoding form params.

## 1.2.15
* Add support for FormEncoded params.
* Add gzip encoding/decoding support.
* Ensure all responses are unzipped correctly.

## 1.2.14
* Fix duplicated connection in HttpClient#execute.

## 1.2.13
* Fix issue where content-types were not matched when a charset was present.

## 1.2.12
* Fix issue where numeric types were deserialized incorrectly.

## 1.2.11
* Bumping version.

## 1.2.10
* Support text/* Content-Type natively.
* Release scripting using releasinator.
* Add serialization annotation, remove serialize/deserialize interfaces.

## 1.2.9
* Update release step to release modules separately.

## 1.2.8
* Update nexus-staging gradle plugin.

## 1.2.7
* added release scripting and changelog.
