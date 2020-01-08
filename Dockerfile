FROM ekidd/rust-musl-builder:1.39.0-openssl11 AS build

COPY Cargo.toml Cargo.lock ./
COPY src/ ./src/
ARG VERSION
RUN set -xe ; \
    test -z "$VERSION" || sed -i -e "/\[package]/,/\[dependencies]/{s/version = \".*\"/version= \"$VERSION\"/}" Cargo.toml ; \
    cargo install --target x86_64-unknown-linux-musl --path .

# Now for the runtime image
FROM scratch

COPY --from=build  /home/rust/.cargo/bin/wait-for-url /wait-for-url

USER 1000

ENTRYPOINT ["/wait-for-url"]
CMD ["--help"]
