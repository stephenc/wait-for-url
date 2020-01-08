# Wait for URL

A command line utility that waits until an URL (or a list of URLs) is ready, i.e. returns HTTP/20x

## Command line options

```
wait-for-url [options] [[--timeout=TIMEOUT] url...]

Waits until all the supplied URLs return HTTP/20x
When multiple URLs are provided they are checked serially.

Options:
  --help             Show this screen
  --allow-empty      Exit normally if given an empty list of URLs to check
  --timeout=TIMEOUT  Changes the timeout for all following URLs to TIMEOUT seconds
                     (default: 300)

Exit codes:
  0  All supplied URLs have returned HTTP/20x at least once
  1  You didn't supply valid command line arguments
  2  One of the supplied URLs did not return HTTP/20x in the required time.
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
            - "--timeout=100"
            - "http://some.url.example.com"
            - "http://another.url.example.com"
            - "--timeout=10"
            - "http://the.last.url.example.com"
```

## Release instructions

To release:

```
mvn release:clean git-timestamp:setup-release release:prepare release:perform
git reset --hard origin/master
git push origin --tags
```
