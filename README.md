json-api-gen
============

Java JSON API generator uses annotated Java classes to generate a JSON API

Methods must be annotated with at least one of the following to be considered a JSON property:

* `com.fasterxml.jackson.annotation.JsonProperty`
* `com.fasterxml.jackson.annotation.JsonView`
