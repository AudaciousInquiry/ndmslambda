# NDMS Lambdas

## What Is This?

This repository holds code for any AWS Lambda Functions used in the NDMS project setup.

These are basically used at this time to automate the gathering and processing of data as well as the generation and publishing of reports.

## How Do I Run These?

You will need, obviously, an **AWS Account** if you wish to execute these in AWS.  You'll also need an AWS account to run locally with [aws sam](https://aws.amazon.com/serverless/sam/) locally which will be reviewed a bit later in this document.

### How Do I Run/Debug These Locally?

You will need:

* AWS Account
* [AWS CLI](https://aws.amazon.com/cli/)
* [AWS SAM](https://aws.amazon.com/serverless/sam/)
* Java 21
* Maven (3.9.9 was used while developing)

## How Do I Deploy These?

Terraform scripts are in the saner-terraform project.
