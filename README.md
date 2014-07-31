Found [Swagger](https://github.com/wordnik/swagger-core), an alternative

json-api-gen
============

### JSON API Generator

Java JSON API generator uses annotated Java classes to generate a JSON API index in html/markdown

A method must be annotated with at least one of the following to be considered a JSON property:

* `com.fasterxml.jackson.annotation.JsonProperty`
* `com.fasterxml.jackson.annotation.JsonView`


### REST API Generator

Java REST API generator uses annotated Java classes to generate a REST API index in html/markdown

A class must be annotated with `javax.ws.rs.Path` in order to be considered a RESTful resource.  Only methods belonging a RESTful resource are parsed.

A method must be annotated with exactly one of the following HTTP methods to be included in the REST API:

* `javax.ws.rs.HEAD`
* `javax.ws.rs.OPTIONS`
* `javax.ws.rs.GET`
* `javax.ws.rs.POST`
* `javax.ws.rs.PUT`
* `javax.ws.rs.DELETE`

If the method is also annotated with `javax.ws.rs.Path`, this path will be appended to the containing resource's path.
