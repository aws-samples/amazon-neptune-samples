import {expect as expectCDK, matchTemplate, MatchStyle } from '@aws-cdk/assert';
import {App} from 'aws-cdk-lib';
import * as SampleDbs from '../lib/sample-dbs-stack';

test('Empty Stack', () => {
    const app = new App();
    // WHEN
    const stack = new SampleDbs.SampleDbsStack(app, 'MyTestStack');
    // THEN
    expectCDK(stack).to(matchTemplate({
      "Resources": {}
    }, MatchStyle.EXACT))
});
