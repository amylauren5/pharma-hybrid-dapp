const Migrations = artifacts.require("Migrations");
const FoodCart = artifacts.require("FoodCartContract");
const Product = artifacts.require("ManufacturerBatchContract.sol.sol.sol.sol.sol");

module.exports = function (deployer) {
    deployer.deploy(Migrations);
    deployer.deploy(FoodCart);
    deployer.deploy(Product);
};