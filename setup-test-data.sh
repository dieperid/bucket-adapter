#!/bin/bash
# setup-test-data.sh
# Creates a 'data/' folder with sample files for testing

# Create the data directory if it doesn't exist
mkdir -p data

# Create 5 sample files
echo "Hello World" > data/file.md
echo "Sample content for file1" > data/file1.txt
echo "Sample content for file2" > data/file2.txt
echo "This is a test file" > data/file3.txt
echo "Another sample file" > data/file4.txt

# Confirmation message
echo "5 sample files created in ./data/"
