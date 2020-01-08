extern crate reqwest;
extern crate shellexpand;

use std::cmp::max;
use std::thread::sleep;
use std::time::{Duration, SystemTime};
use std::{env, process};

use getopts::Options;
use reqwest::Client;

fn create_options() -> Options {
    let mut opts = Options::new();
    opts.optflag("h", "help", "print this help menu and exit");
    opts.optflag("V", "version", "print the version and exit");
    opts.optopt("w", "wait", "the number of seconds to wait until a successful response is received, specify a value of 0 to disable retries (default: 300)", "SEC");
    opts.optopt(
        "i",
        "interval",
        "the number of milliseconds to wait between attempts (default: 250)",
        "MILLISECONDS",
    );
    opts.optflag(
        "E",
        "allow-empty",
        "suppress error if the list of URLs is empty",
    );
    opts
}

fn print_usage(program: &str, opts: Options) {
    let brief = format!("Usage: {} [options] urls", program);
    println!("{}", opts.usage(&brief));
    println!();
}

fn check_url(url: &str, timeout_secs: u64, interval_millis: u64) {
    println!("Checking {}", url);
    let client = Client::builder()
        .timeout(Duration::from_secs(5))
        .connect_timeout(Duration::from_secs(1))
        .build().unwrap();
    let start = SystemTime::now();
    let mut last_status: i32 = -1;
    let mut last_update = SystemTime::now();
    loop {
        // make the request
        match client.get(url).send() {
            Ok(response) => match response.status().as_u16() / 100 {
                2 => {
                    println!("  HTTP/{}", response.status());
                    return;
                }
                _ => {
                    if i32::from(response.status().as_u16()) != last_status {
                        last_status = i32::from(response.status().as_u16());
                        last_update = SystemTime::now();
                        println!("  HTTP/{}", response.status())
                    } else {
                        match last_update.elapsed() {
                            Ok(elapsed) => {
                                if elapsed.as_secs() > 5 {
                                    last_update = SystemTime::now();
                                    println!("  HTTP/{}", response.status())
                                }
                            }
                            Err(_) => println!("  HTTP/{}", response.status()),
                        }
                    }
                }
            },
            Err(e) => match &e.status() {
                Some(status) => {
                    if i32::from(status.as_u16()) != last_status {
                        last_status = i32::from(status.as_u16());
                        last_update = SystemTime::now();
                        println!("  HTTP/{}", status)
                    } else {
                        match last_update.elapsed() {
                            Ok(elapsed) => {
                                if elapsed.as_secs() > 5 {
                                    last_update = SystemTime::now();
                                    println!("  HTTP/{}", status)
                                }
                            }
                            Err(_) => println!("  HTTP/{}", status),
                        }
                    }
                }
                _ => {
                    if last_status != -2 {
                        last_status = -2;
                        last_update = SystemTime::now();
                        println!("  {:?}", e)
                    } else {
                        match last_update.elapsed() {
                            Ok(elapsed) => {
                                if elapsed.as_secs() > 5 {
                                    last_update = SystemTime::now();
                                    println!("  {:?}", e)
                                }
                            }
                            Err(_) => println!("  {:?}", e),
                        }
                    }
                }
            },
        };
        match start.elapsed() {
            Ok(elapsed) => {
                if elapsed.as_secs() > timeout_secs {
                    println!("  TIMEOUT");
                    process::exit(2);
                }
            }
            Err(e) => println!("  Unexpected error: {:?}", e),
        }
        // sleep for
        sleep(Duration::from_millis(max(interval_millis, 1)));
    }
}

fn main() {
    // set up to parse the command line options
    const VERSION: &'static str = env!("CARGO_PKG_VERSION");
    let args: Vec<String> = env::args().collect();
    let program = args[0].clone();

    let opts = create_options();
    let matches = match opts.parse(&args[1..]) {
        Ok(m) => m,
        Err(f) => panic!(f.to_string()),
    };

    // process and validate the command line options
    if matches.opt_present("h") {
        print_usage(&program, opts);
        return;
    }
    if !matches.opt_present("E") && matches.free.is_empty() {
        eprintln!("Expected a list of URLs");
        process::exit(1);
    }
    if matches.opt_present("V") {
        println!("{}", VERSION);
        return;
    }

    let wait = match matches.opt_str("w") {
        Some(wait_str) => wait_str.parse().expect("Wait time should be an integer"),
        None => 300,
    };

    let interval = match matches.opt_str("i") {
        Some(interval_str) => interval_str.parse().expect("Interval should be an integer"),
        None => 250,
    };

    for url in matches.free.iter() {
        let expanded_url = match shellexpand::env(url) {
            Ok(x) => x.to_string(),
            Err(_) => url.to_string(),
        };
        check_url(expanded_url.as_str(), wait, interval);
    }
    println!("OK")
}
