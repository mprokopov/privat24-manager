#!/bin/bash
lein uberjar
docker-compose build
