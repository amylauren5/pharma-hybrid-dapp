// SPDX-License-Identifier: MIT
pragma solidity ^0.8.10;

import "./ManufacturerBatchContract.sol";
import "./TrackingBatchContract.sol";

contract DistributorBatchContract {
    TrackingBatchContract private tracking;
    ManufacturerBatchContract private manufacturerBatch;
    address public owner;

    // Struct to represent distributor products
    struct DistributorBatch {
        ManufacturerBatchContract.Batch batchDetails; // Manufacturer product details
        uint256 distributorPrice; // Revised price of the manufactured product
        uint256 quantityAvailable; // Quantity available for distribution
        address distributor; // Address of the distributor
        string distributorLocation; // Location of the distributor
    }

    // Mapping to store distributor products by their batch IDs
    mapping(string => DistributorBatch) private distributorProducts;

    // Mapping to store valid transitions
    mapping(TrackingBatchContract.State => mapping(TrackingBatchContract.State => bool)) private validTransitions;

    // Events
    event DistributorBatchAdded(string indexed batchID, address indexed distributor);
    event TrackingUpdated(bytes32 indexed trackingID, TrackingBatchContract.State newState);
    event MedicineSoldToConsumer(string indexed batchID, address receiver, uint256 quantity);

    // Modifier to check if the caller is a registered distributor
    modifier onlyOwner() {
        require(msg.sender == owner, "Not the contract owner");
        _;
    }

    // Constructor to initialise the ManufacturerBatchContract.sol contract reference
    constructor(address _trackingContractAddress, address _manufacturerContractAddress) {
        require(_trackingContractAddress != address(0), "Invalid tracking contract address");
        require(_manufacturerContractAddress != address(0), "Invalid manufacturer contract address");

        // Set contract addresses
        tracking = TrackingBatchContract(_trackingContractAddress);
        manufacturerBatch = ManufacturerBatchContract(_manufacturerContractAddress);

        // Initialize valid state transitions
        initialiseValidTransitions();
    }

    // Private function to initialize valid transitions
    function initialiseValidTransitions() private {
        validTransitions[TrackingBatchContract.State.ShippedToDistributor][TrackingBatchContract.State.DeliveredToDistributor] = true;
        validTransitions[TrackingBatchContract.State.DeliveredToDistributor][TrackingBatchContract.State.SoldToConsumer] = true;
        validTransitions[TrackingBatchContract.State.SoldToConsumer][TrackingBatchContract.State.ShippedToConsumer] = true;
    }

    // Function to add a new distributor product
    function addDistributorBatch(
        bytes32 _trackingID,
        uint256 _distributorPrice,
        uint256 _quantityAvailable,
        string memory _distributorLocation
    ) public onlyOwner() {
        // Verify tracking exists
        TrackingBatchContract.Tracking memory trackingStruct = tracking.getTracking(_trackingID);
        require(trackingStruct.trackingID != bytes32(0));

        // Get batch ID from tracking
        string memory batchID = trackingStruct.batchID;

        // Validate existence of product and verify state
        ManufacturerBatchContract.Batch memory batch = manufacturerBatch.getBatch(batchID);
        require(batch.manufacturer != address(0), "Manufacturer batch does not exist"); // Check if manufacturer address is valid
        require(tracking.getCurrentTrackingState(_trackingID) == TrackingBatchContract.State.DeliveredToDistributor,
            "Manufacturer batch is not available for distribution"
        );

        // Create the distributor product
        distributorProducts[batchID] = DistributorBatch({
            batchDetails: batch,
            distributorPrice: _distributorPrice,
            quantityAvailable: _quantityAvailable,
            distributor: msg.sender,
            distributorLocation: _distributorLocation
        });

        // Emit event
        emit DistributorBatchAdded(batchID, msg.sender);
    }

    // Helper function for updating state
    function isValidTransition(TrackingBatchContract.State _currentState, TrackingBatchContract.State _newState) internal view returns (bool) {
        return validTransitions[_currentState][_newState];
    }

    // Function to sell medicine to receiver
    function sellMedicineToConsumer(
        bytes32 _trackingID,
        string memory _batchID,
        address _consumer,
        uint256 _quantity
    ) public onlyOwner() {
        DistributorBatch storage product = distributorProducts[_batchID];
        // Ensure the distributor product exists
        require(product.distributor != address(0), "Product does not exist");

        // Validate quantity then deduct
        require(_quantity > 0, "Quantity must be greater than zero");
        require(_quantity <= product.quantityAvailable, "Quantity of medicine not available for sale");
        product.quantityAvailable -= _quantity;

        // Update tracking with new information
        tracking.updateTrackingByDistributor(_trackingID, msg.sender, _consumer);

        // Emit event
        emit MedicineSoldToConsumer(_batchID, _consumer, _quantity);
    }

    // Function to update distributor tracking state
    function updateDistributorTrackingState(bytes32 _trackingID, TrackingBatchContract.State _newState) public onlyOwner() {
        require(_trackingID != bytes32(0), "Tracking ID not set");

        TrackingBatchContract.State currentState = tracking.getCurrentTrackingState(_trackingID);
        require(isValidTransition(currentState, _newState), "Invalid state transition");
        require(_newState != TrackingBatchContract.State.SoldToConsumer, "This transition is automated");

        // Handle case when receiver cancels purchase
        if (_newState == TrackingBatchContract.State.CancelledByConsumer) {
            require(currentState != TrackingBatchContract.State.ShippedToConsumer, "Cannot cancel after shipping");

            // Retrieve batch ID and ensure product exists
            TrackingBatchContract.Tracking memory trackingData = tracking.getTracking(_trackingID);
            string memory _batchID = trackingData.batchID;
            require(distributorProducts[_batchID].distributor != address(0), "Distributor product does not exist");
        }

        // Update tracking state
        tracking.updateTrackingState(_trackingID, _newState);

        // Emit event
        emit TrackingUpdated(_trackingID, _newState);
    }

    // Function to retrieve distributor product details
    function getDistributorBatch(string memory _batchID) public view returns (DistributorBatch memory) {
        require(distributorProducts[_batchID].distributor != address(0), "Distributor product does not exist"); // Check if distributor address is valid
        return distributorProducts[_batchID];
    }
}