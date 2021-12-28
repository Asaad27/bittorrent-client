package main

import (
	"fmt"
	"log"
	"os"
	"strconv"
	"strings"
)

//file1= {1, 100}
//file2 = {100, 500}
//file3 = {500, 900}
//file4 = {900, 100}
//file5 = {1, 1000}


//input : size of piece in KB, number of pieces, begin-end

var sizeOfPiece = 256 * 1024
var numberOfPieces = 1000
var sizeOfFile = int64(numberOfPieces * sizeOfPiece)

func makeFile(fileName string, numberOfPiecesToWrite int, offset int){

	f, err := os.Create(fileName)
	if err != nil {
		log.Fatal(err)
	}
	if err := f.Truncate(sizeOfFile); err != nil {
		log.Fatal(err)
	}

	s := strings.Repeat("1", numberOfPiecesToWrite)
	_, err = f.WriteAt([]byte(s), int64(offset))
	if err != nil{
		log.Fatalf("failed to write")
	}
}

func main() {
	fileName := os.Args[1]
	numberOfPiecesToWrite, _ := strconv.Atoi(os.Args[2])
	offset, _ := strconv.Atoi(os.Args[3])
	fmt.Println("fileName : " + fileName + " nofPieces : " + strconv.FormatInt(int64(numberOfPiecesToWrite), 10) + " offset: " + strconv.FormatInt(int64(offset), 10))
	makeFile(fileName, numberOfPiecesToWrite, offset-1)
}


