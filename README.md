cascading-release-maven-plugin
==============================

Multi-project Maven release plugin, for releasing groups of projects together in one click

## Why would I use this plugin?
Suppose you have a complex Maven project structure, with many dependencies between modules. Suppose releasing is not a matter of one simple "mvn release:prepare release:perform" command, because all modules need to be released separately. See picture below.

![alt text](https://github.com/jankeesvanandel/cascading-release-maven-plugin/raw/master/src/common/images/icon48.png "Logo Title Text 1")

This is unneccessary manual labour and also error prone, depending on the amount of modules you need to release.

## How does it work?
