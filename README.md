# Wait for URL

A command line utility that waits until an URL (or a list of URLs) is ready, i.e. returns HTTP/20x

## Command line options

```
Usage: wait-for-url [options] urls

Options:
    -h, --help          print this help menu and exit
    -V, --version       print the version and exit
    -w, --wait SEC      the number of seconds to wait until a successful
                        response is received, specify a value of 0 to disable
                        retries (default: 300)
    -i, --interval MILLISECONDS
                        the number of milliseconds to wait between attempts
                        (default: 250)
    -E, --allow-empty   suppress error if the list of URLs is empty
```

## Example usage

Use as an `initContainer` so that your pod doesn't start until required services are available, e.g.

```yaml
apiVersion: apps/v1
kind: Deployment
#...
spec:
  #...
  template:
    #...
    spec:
      #...
      initContainers:
        - name: check-ready
          image: stephenc/wait-for-url
          args:
            - "/wait-for-url"                 
            - "--wait=100"
            - "http://some.url.example.com"
            - "http://another.url.example.com"
            - "http://${EXAMPLE_DYNAMIC_HOST}:${EXAMPLE_DYNAMIC_PORT}"
            - "http://the.last.url.example.com" 
          env:
            - name: EXAMPLE_DYNAMIC_HOST
              valueFrom:
                configMapKeyRef:
                  #...
            - name: EXAMPLE_DYNAMIC_PORT
              valueFrom:
                configMapKeyRef:
                  #...
``` 

*NOTE:* any usage of `${NAME}` will be attempted to be expanded from the environment by `wait-for-url`. This is important as the container doesn't have a shell to perform shell expansion for you. This is not full featured shell expansion, just super simple expansion.

## Release instructions

To release:

* Update version in `./Cargo.toml`
* Commit
* Create a tag called `wait-for-url-$VERSION`
* Push tag and master 
