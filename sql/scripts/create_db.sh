#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
export DB_NAME=$USER"_DB"

psql -h localhost -p $PGPORT $DB_NAME < $DIR/../src/create_tables.sql
psql -h localhost -p $PGPORT $DB_NAME < $DIR/../src/create_index.sql
psql -h localhost -p $PGPORT $DB_NAME < $DIR/../src/load_data.sql
