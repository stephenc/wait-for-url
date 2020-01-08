package com.github.stephenc.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class WaitForUrl {

    public static void main(String... urls) {
        // check for help and sanity check
        boolean haveUrl = false;
        boolean emptyOk = false;
        for (String arg : urls) {
            switch (arg.toLowerCase(Locale.ENGLISH)) {
                case "-h":
                case "--help":
                case "-?":
                case "help":
                    showHelp();
                    return;
                case "--allow-empty":
                    emptyOk = true;
                    break;
                default:
                    if (arg.startsWith("http://") || arg.startsWith("https://")) {
                        haveUrl = true;
                    }
                    break;
            }
        }
        if (!haveUrl) {
            if (emptyOk) {
                System.out.println("Nothing to check.");
                return;
            }
            showHelp();
            System.exit(1);
        }
        int timeout = 300;
        Iterator<String> iterator = Arrays.asList(urls).iterator();
        while (iterator.hasNext()) {
            String urlString = iterator.next();
            if (urlString.startsWith("http://") || urlString.startsWith("https://")) {
                System.out.print("Checking " + urlString + " ");
                System.out.flush();
                try {
                    URL url = new URL(urlString);
                    long start = System.nanoTime();
                    long waitTime = timeout > 0 ? TimeUnit.SECONDS.toNanos(timeout) : Long.MAX_VALUE;
                    long remaining;
                    long lastElipsis = start;
                    Integer responseCode = null;
                    while (((remaining = System.nanoTime() - start) < waitTime)) {
                        if (System.nanoTime() - lastElipsis > TimeUnit.SECONDS.toNanos(10)) {
                            lastElipsis += TimeUnit.SECONDS.toNanos(10);
                            System.out.print('.');
                            System.out.flush();
                        }
                        try {
                            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                            final long limit = Math.max(100, Math.min(3000, TimeUnit.NANOSECONDS.toMillis(remaining)));
                            connection.setConnectTimeout((int) limit);
                            connection.setReadTimeout((int) limit);
                            connection.getInputStream().close();
                            final int status = connection.getResponseCode();
                            if (responseCode == null || responseCode != status) {
                                System.out.print(" HTTP/" + status);
                                System.out.flush();
                            }
                            responseCode = status;
                            connection.disconnect();
                            if (responseCode / 100 == 2) {
                                break;
                            }
                        } catch (ConnectException e) {
                            if (responseCode != null) {
                                System.out.print(" REFUSED");
                                System.out.flush();
                                responseCode = null;
                            }
                        } catch (FileNotFoundException e) {
                            if (responseCode == null || responseCode != 404) {
                                System.out.print(" HTTP/404");
                                System.out.flush();
                            }
                            responseCode = 404;
                        } catch (IOException e) {
                            if (responseCode != null) {
                                System.out.print(" TIMEOUT");
                                System.out.flush();
                                responseCode = null;
                            }
                        }
                    }
                    System.out.println();
                    if (responseCode == null) {
                        System.out.println("No response from " + urlString + " after " + timeout + " seconds");
                        System.exit(2);
                    }
                    if (responseCode / 100 != 2) {
                        System.out.println(
                                "Last response from " + urlString + " after " + timeout + " seconds was HTTP/"
                                        + responseCode);
                        System.exit(2);
                    }

                } catch (MalformedURLException e) {
                    System.err.println("Supplied URL " + urlString + " is invalid: " + e.getMessage());
                    System.exit(1);
                }
            } else if (urlString.startsWith("-")) {
                if (urlString.startsWith("--timeout=")) {
                    try {
                        timeout = Integer.parseInt(urlString.substring("--timeout=".length()));
                    } catch (NumberFormatException e) {
                        System.err.println("Supplied timeout value " + urlString.substring("--timeout=".length())
                                + "is not a number");
                        System.exit(1);
                    }
                    System.out.println("Changed timeout to " + (timeout > 0 ? timeout : "disabled"));
                } else if (urlString.equals("--timeout")) {
                    if (!iterator.hasNext()) {
                        System.err.println("Expected a timeout after --timeout");
                        System.exit(1);
                    }
                    urlString = iterator.next();
                    try {
                        timeout = Integer.parseInt(urlString);
                    } catch (NumberFormatException e) {
                        System.err.println("Supplied timeout value " + urlString + "is not a number");
                        System.exit(1);
                    }
                    System.out.println("Changed timeout to " + (timeout > 0 ? timeout : "disabled"));
                }
            } else {
                System.err.println("Expected an URL, got " + urlString);
                System.exit(1);
            }
        }
        System.out.println("OK");
    }

    private static void showHelp() {
        System.out.println("wait-for-url [options] [[--timeout=TIMEOUT] url...]");
        System.out.println();
        System.out.println("Waits until all the supplied URLs return HTTP/20x");
        System.out.println("When multiple URLs are provided they are checked serially.");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --help             Show this screen");
        System.out.println("  --allow-empty      Exit normally if given an empty list of URLs to check");
        System.out.println("  --timeout=TIMEOUT  Changes the timeout for all following URLs to TIMEOUT seconds");
        System.out.println("                     (default: 300)");
        System.out.println();
        System.out.println("Exit codes:");
        System.out.println("  0  All supplied URLs have returned HTTP/20x at least once");
        System.out.println("  1  You didn't supply valid command line arguments");
        System.out.println("  2  One of the supplied URLs did not return HTTP/20x in the required time.");
    }
}
