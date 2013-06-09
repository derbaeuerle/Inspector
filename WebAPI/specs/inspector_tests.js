describe("Inspector Testsuite", function() {

    var data_json, data_null_json, data_error;
    
    beforeEach(function() {
        data_json = [0, 1, 2, 3];
        data_null_json = [];
        data_error = '{ type: "error", stacktrace: "Go look for it!" }';
    });

    it("Inspector defined", function() {
        expect(inspector).toBeDefined();
    });

    it("Testing" ,function() {
        expect(data_json).toBeDefined();
    });

});