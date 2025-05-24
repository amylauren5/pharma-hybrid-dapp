// SPDX-License-Identifier: MIT
pragma solidity ^0.8.10;

import {TrackingBatchContract} from "./TrackingBatchContract.sol";

contract ManufacturerBatchContract {
    TrackingBatchContract private tracking;
    address public owner;

    // Struct to represent manufacturer products
    struct Batch {
        string batchID; // Identifier for batch
        bytes32 trackingID; // Identifier for tracking
        string productCode; // Code of the product
        string name; // Name of the product
        string description; // Description of the product
        uint256 expiringDate; // Expiration date of the product
        uint256 totalQuantity; // Total quantity in the batch
        uint256 price; // Price in Ether
        address manufacturer; // Address of manufacturer
        string manufacturerLocation; // Location of manufacturer
    }

    // A mapping to store products by their batchID
    mapping(string => Batch) private batches;

    // Mapping to store valid transitions
    mapping(TrackingBatchContract.State => mapping(TrackingBatchContract.State => bool)) private validTransitions;

    // Events
    event TrackingAdded(string indexed batchID, string name, address indexed manufacturer);
    event TrackingUpdated(bytes32 indexed trackingID, TrackingBatchContract.State newState);
    event MedicineSoldToDistributor(string indexed batchID, address distributor, uint256 quantity);

    // Modifier to check if the caller is the manufacturer of the product
    modifier onlyOwner() {
        require(msg.sender == owner, "Not the contract owner");
        _;
    }

    // Constructor
    constructor(address _trackingContractAddress){
        tracking = TrackingBatchContract(_trackingContractAddress);
        owner = msg.sender;
        initialiseValidTransitions();
    }

    // Private function to initialize valid transitions
    function initialiseValidTransitions() private {
        validTransitions[TrackingBatchContract.State.Manufactured][TrackingBatchContract.State.SoldToDistributor] = true;
        validTransitions[TrackingBatchContract.State.SoldToDistributor][TrackingBatchContract.State.ShippedToDistributor] = true;
    }

    function generateUUID() public view returns (bytes32) {
        return keccak256(abi.encodePacked(msg.sender, block.timestamp));
    }

    // Function to add a new product
    function addMedicineBatch(
        string memory _batchID,
        string memory _productCode,
        string memory _name,
        string memory _description,
        uint256 _expiringDate,
        uint256 _totalQuantity,
        uint256 _price,
        string memory _manufacturerLocation
    ) public onlyOwner() {
        require(batches[_batchID].manufacturer == address(0), "Batch ID already exists");
        require(_expiringDate > block.timestamp, "Expiration date must be in the future");
        require(_totalQuantity > 0, "Total quantity must be greater than zero");

        bytes32 uuid = generateUUID();

        batches[_batchID] = Batch({
            batchID: _batchID,
            trackingID: uuid,
            productCode: _productCode,
            name: _name,
            description: _description,
            expiringDate: _expiringDate,
            totalQuantity: _totalQuantity,
            price: _price,
            manufacturer: msg.sender,
            manufacturerLocation: _manufacturerLocation
        });

        tracking.createTracking(uuid, _batchID, msg.sender,
            address(0), address(0),_manufacturerLocation,
            "");

        // Emit event
        emit TrackingAdded(_batchID, _name, msg.sender);
    }

    // Function to sell medicine to distributors
    function sellMedicineToDistributor(string memory _batchID, address _distributor,
        string memory _distributorLocation, uint256 _quantity) private onlyOwner() {

        // Validation
        require(batches[_batchID].manufacturer != address(0), "Batch does not exist");
        require(_quantity > 0, "Quantity must be greater than zero");
        require(_quantity <= batches[_batchID].totalQuantity, "Quantity of medicine not available for sale");

        // Update batch _quantity
        batches[_batchID].totalQuantity -= _quantity;

        bytes32 trackingID = batches[_batchID].trackingID;

        // Generate a random hash and create tracking
        tracking.createTracking(
            trackingID,
            _batchID,
            msg.sender,
            address(0),
            _distributor,
            batches[_batchID].manufacturerLocation,
            _distributorLocation
        );

        // Update tracking state
        tracking.updateTrackingState(trackingID, TrackingBatchContract.State.SoldToDistributor);

        // Emit event
        emit MedicineSoldToDistributor(_batchID, _distributor, _quantity);
    }

    // Helper function for updating state
    function isValidTransition(TrackingBatchContract.State _currentState, TrackingBatchContract.State _newState) internal view returns (bool) {
        return validTransitions[_currentState][_newState];
    }

    // Function to update product state
    function updateTrackingState(bytes32 _trackingID, TrackingBatchContract.State _newState) public onlyOwner() {
        require(_trackingID != bytes32(0), "Tracking ID not set");
        TrackingBatchContract.State currentState = tracking.getCurrentTrackingState(_trackingID);
        require(isValidTransition(currentState, _newState),"Invalid state transition");
        require(_newState != TrackingBatchContract.State.SoldToDistributor, "This state transition is automated");

        // Update tracking state
        tracking.updateTrackingState(_trackingID, _newState);

        // Emit event
        emit TrackingUpdated(_trackingID, _newState);
    }

    // Function to retrieve product details
    function getBatch(string memory _batchID) public view returns (Batch memory) {
        require(batches[_batchID].manufacturer != address(0), "Batch does not exist");
        return batches[_batchID];
    }

    // Function to check if a product is expired
    function isBatchExpired(string memory _batchID) public view returns (bool) {
        require(batches[_batchID].manufacturer != address(0), "Batch does not exist");
        return block.timestamp > batches[_batchID].expiringDate;
    }
}
