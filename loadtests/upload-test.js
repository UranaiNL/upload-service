import http from 'k6/http';
import {check} from 'k6';
import {FormData} from 'https://jslib.k6.io/formdata/0.0.2/index.js';

// define configuration
export const options = {
    // define thresholds
    thresholds: {
        http_req_failed: [{ threshold: "rate<0.01", abortOnFail: true }], // availability threshold for error rate
        http_req_duration: ["p(99)<1000"], // Latency threshold for percentile
    },

    // define scenarios
    scenarios: {
        breaking: {
            executor: "ramping-vus",
            stages: [
                { duration: "10s", target: 20 },
                { duration: "50s", target: 20 },
                { duration: "50s", target: 40 },
                { duration: "50s", target: 60 },
                { duration: "50s", target: 80 },
                { duration: "50s", target: 100 },
            ],
        },
    },
};

// Function to generate a unique file name
function generateUniqueFileName(fileName) {
    const timestamp = Date.now(); // Get current timestamp
    return `${timestamp}_${fileName}`;
}

const video = open("./30secslong.mp4");

export default function () {
    const uniqueFileName = generateUniqueFileName('30secslong.mp4'); // Generate unique file name
    const fd = new FormData();
    fd.append('uploaderId', '1')
    fd.append('p1Username', '1t')
    fd.append('p2Username', '2t')
    fd.append('p1CharacterId', '1')
    fd.append('p2CharacterId', '1')
    fd.append('gameId', '1')
    fd.append('video', http.file(video, uniqueFileName, 'video/mp4') );

    const res = http.post('http://localhost:8081/api/upload', fd.body(), {
        headers: { 'Content-Type': 'multipart/form-data; boundary=' + fd.boundary },
    });

    check(res, {
        'is status 200': (r) => r.status === 200,
    });
}
