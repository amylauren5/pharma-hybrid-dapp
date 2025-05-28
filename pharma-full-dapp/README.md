# pharma-full-dapp

## Requirements:
1. Web3j CLI (web3j-1.6.1.zip): https://github.com/hyperledger-web3j/web3j-cli/releases/tag/v1.6.1
2. Truffle suite (refer to https://archive.trufflesuite.com/guides/how-to-install-truffle-and-testrpc-on-windows-for-blockchain-development/)
3. Apache Maven
4. Java 21
5. Javascript

Use npm to install the following:
1. npm install truffle web3
2. npm install chai@4.3.6 chai-as-promised@7.1.1

## Instructions:
1. truffle init (initializes a new Truffle project - migration files should be generated in migrations directory)
2. truffle compile (compiles smart contracts and generates JSON artifacts with ABI and bytecode)
3. Use Web3j CLI tool to generate Java wrappers for the Solidity smart contracts:

web3j generate truffle --truffle-json C:[Your Path]\ICT3914-Final-Year-Project\Web3\build\contracts\Example.json --outputDir C:[Your Path]\ICT3914-Final-Year-Project\Web3\src\main\java\com\web3j --package com.web3j

4. Start Ganache on Quickstart

5. truffle migrate --network development (deploys contracts on Ganache)

Note: Make sure you have the following in truffle-config.js file:

module.exports = {
  networks: {
    development: {
      host: "127.0.0.1",     // Localhost (default: none)
      port: 7545,            // Ganache port (default: 8545)
      network_id: "*",       // Any network (default: *),
    },
  },
  // Other configurations...
};

Run truffle migrate --reset --network development to rerun migrations.

## References: 
- https://www.baeldung.com/web3j
- https://docs.soliditylang.org/en/v0.8.28/solidity-by-example.html
- https://ferdyhape.medium.com/remix-ide-and-ganache-a-beginners-guide-to-smart-contract-deployment-b0df68c48ae6