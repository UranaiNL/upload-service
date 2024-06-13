const fs = require('fs');

function setupMultipartFormDataWithFile(requestParams, context, ee, next) {
    const formData = {
        uploaderId: context.vars.uploaderId,
        p1Username: context.vars.p1Username,
        p2Username: context.vars.p2Username,
        p1CharacterId: context.vars.p1CharacterId,
        p2CharacterId: context.vars.p2CharacterId,
        gameId: context.vars.gameId,
    };

    const video = fs.readFileSync('30secslong.mp4');
    formData.video = video

    requestParams.formData = formData;
    return next();
}

module.exports = {
    setupMultipartFormDataWithFile,
};