package main

import (
	"bytes"
	"fmt"
	"log"
	"math"
	"os"
	"strconv"
)


var sizeOfPiece = int64(256 * 1024)
var numberOfPieces = int64(1000)
var sizeOfFile = 0

func makeFile(fileName string, numPieces int64, offset int64) {
	from := strconv.FormatInt(offset, 10)
	end := strconv.FormatInt(offset+numPieces-1, 10)
	offset -= 1
	path := "peer_" + from + "_to_" + end

	if _, err := os.Stat(path); os.IsNotExist(err) {
		err := os.Mkdir(path, 0755)
		if err != nil {
			log.Fatal(err)
		}
	}

	f, err := os.Create(path + "/" + fileName)
	if err != nil {
		log.Fatal(err)
	}
	if err := f.Truncate(int64(sizeOfFile)); err != nil {
		log.Fatal(err)
	}

	_, err = f.WriteAt(bytes.Repeat([]byte("a"), int(numPieces*sizeOfPiece)), offset*sizeOfPiece)

	_, err = f.WriteAt(bytes.Repeat([]byte("x"), int(offset*sizeOfPiece)), 0)
	_, err = f.WriteAt(bytes.Repeat([]byte("x"), int((numberOfPieces-(offset+numPieces))*sizeOfPiece)), (offset+numPieces)*sizeOfPiece)
	if err != nil {
		log.Fatalf("failed to write")
	}
}

func main() {
	fileName := os.Args[1]
	from, _ := strconv.Atoi(os.Args[2])
	to, _ := strconv.Atoi(os.Args[3])
	if len(os.Args) > 4{
		var err error
		var argValue int
		argValue, err = strconv.Atoi(os.Args[4])
		numberOfPieces = int64(argValue)
		if err != nil {
			log.Fatalf("error parsing last argument, verify number of pieces")
		}
	}
	if len(os.Args) > 5{
		var err error
		var argValue int
		argValue, err = strconv.Atoi(os.Args[5])
		sizeOfPiece = int64(argValue)
		if err != nil{
			log.Fatalf("error parsing last argument")
		}
	}
	sizeOfFile = int(sizeOfPiece * numberOfPieces)
	to = int(math.Min(float64(numberOfPieces), float64(to)))
	from = int(math.Max(1, float64(from)))
	numberOfPiecesToWrite := to - from + 1
	offset := from
	fmt.Printf("size of piece : %v\t number of pieces to write: %v\t size of file : %v\n",sizeOfPiece, numberOfPiecesToWrite, sizeOfFile )

	fmt.Println("fileName : " + fileName + " \tPieces: " + strconv.FormatInt(int64(from), 10) + " -> " + strconv.FormatInt(int64(to), 10))
	makeFile(fileName, int64(numberOfPiecesToWrite), int64(offset))

}
