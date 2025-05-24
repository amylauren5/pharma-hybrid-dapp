// SPDX-License-Identifier: MIT
pragma solidity ^0.8.10;

contract TrackingBatchContract {
    // Enum for product state
    enum State { Manufactured, SoldToDistributor, ShippedToDistributor, DeliveredToDistributor,
        SoldToConsumer, ShippedToConsumer, DeliveredToConsumer, CancelledByConsumer }

    // Struct holding tracking information
    struct Tracking {
        bytes32 trackingID; // ID of the tracking process
        string batchID; // ID of the batch
        address manufacturer; // Address of manufacturer
        address distributor; // Address of distributor
        address consumer; // Address of consumer
        uint256 manufacturingTimestamp; // Timestamp of manufacturing
        uint256 shippingTimestamp; // Timestamp of shipping
        uint256 deliveryTimestamp; // Timestamp of delivery
        string manufacturerLocation; // Location of manufacturer
        string distributorLocation; // Location of distributor
        State[] states; // States of the tracking process e.g. Manufactured, Shipped, Delivered
    }

    // A mapping to store products by their tracking ID
    mapping(bytes32 => Tracking) private trackings;

    // Mapping to store valid transitions
    mapping(State => mapping(State => bool)) private validTransitions;

    // Events for creating and updating tracking

    event TrackingCreated(bytes32 indexed trackingID, address indexed manufacturer,
        address indexed distributor, address consumer);

    event TrackingStateUpdated(bytes32 indexed trackingID, State newState);

    event TrackingUpdatedByDistributor(bytes32 indexed trackingID,
        address indexed distributor, address consumer);

    // Constructor
    constructor() {
        initialiseValidTransitions();
    }

    // Private function to initialise valid transitions
    function initialiseValidTransitions() private {
        validTransitions[State.Manufactured][State.SoldToDistributor] = true;
        validTransitions[State.SoldToDistributor][State.ShippedToDistributor] = true;
        validTransitions[State.ShippedToDistributor][State.DeliveredToDistributor] = true;
        validTransitions[State.DeliveredToDistributor][State.SoldToConsumer] = true;
        validTransitions[State.SoldToConsumer][State.ShippedToConsumer] = true;
        validTransitions[State.SoldToConsumer][State.CancelledByConsumer] = true;
        validTransitions[State.ShippedToConsumer][State.DeliveredToConsumer] = true;
    }

    // Function to create a tracking record
    function createTracking(
        bytes32 _trackingID,
        string memory _batchID,
        address _manufacturer,
        address _distributor,
        address _consumer,
        string memory _manufacturerLocation,
        string memory _distributorLocation
    ) external {
        State[] memory _states;

        // Create the tracking with timestamps initialised
        trackings[_trackingID] = Tracking({
            trackingID: _trackingID,
            batchID: _batchID,
            manufacturer: _manufacturer,
            distributor: _distributor,
            consumer: _consumer,
            manufacturingTimestamp: block.timestamp, // Set to the current timestamp
            shippingTimestamp: 0, // Initialised as 0 until shipped
            deliveryTimestamp: 0, // Initialised as 0 until delivered
            states: _states,
            manufacturerLocation: _manufacturerLocation,
            distributorLocation: _distributorLocation
        });

        trackings[_trackingID].states.push(State.Manufactured);

        // Emit event
        emit TrackingCreated(_trackingID, _manufacturer, _distributor, _consumer);
    }

    // Helper function for updating state
    function isValidTransition(State _currentState, State _newState) internal view returns (bool) {
        return validTransitions[_currentState][_newState];
    }

    // Helper function to validate tracking ID
    function isValidTrackingID(bytes32 _trackingID) private view returns (bool) {
        return trackings[_trackingID].trackingID != bytes32(0);
    }

    // Function to update tracking data
    function updateTrackingByDistributor(bytes32 _trackingID, address _distributor, address _consumer) external {
        trackings[_trackingID].distributor = _distributor;
        trackings[_trackingID].consumer = _consumer;
        trackings[_trackingID].states.push(State.SoldToConsumer);
        emit TrackingUpdatedByDistributor(_trackingID, _distributor, _consumer);
    }

    // Function to update the tracking state
    function updateTrackingState(bytes32 _trackingID, State _newState) public {
        Tracking storage tracking = trackings[_trackingID];
        require(tracking.trackingID != bytes32(0), "Tracking does not exist");

        State currentState = getCurrentTrackingState(_trackingID);
        require(isValidTransition(currentState, _newState), "Invalid state transition");

        // Update relevant timestamps
        if (
            _newState == State.ShippedToDistributor ||
            _newState == State.ShippedToConsumer
        ) {
            tracking.shippingTimestamp = block.timestamp;
        } else if (
            _newState == State.DeliveredToDistributor ||
            _newState == State.DeliveredToConsumer
        ) {
            tracking.deliveryTimestamp = block.timestamp;
        }

        tracking.states.push(_newState);
        emit TrackingStateUpdated(_trackingID, _newState);
    }

    // GETTER FUNCTIONS

    // Function to fetch tracking details
    function getTracking(bytes32 _trackingID) public view returns (Tracking memory) {
        return trackings[_trackingID];
    }

    // Function to fetch current tracking state
    function getCurrentTrackingState(bytes32 _trackingID) public view returns (State) {
        Tracking memory latestTracking = getTracking(_trackingID);
        require(latestTracking.states.length > 0, "No states available");
        uint lastIndex = latestTracking.states.length - 1;
        return latestTracking.states[lastIndex];
    }

    // Function to fetch all tracking states
    function getAllTrackingStates(bytes32 _trackingID) public view returns (State[] memory) {
        return getTracking(_trackingID).states;
    }
}
