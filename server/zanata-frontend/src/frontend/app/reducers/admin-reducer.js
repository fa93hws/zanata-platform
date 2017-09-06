import {handleActions} from 'redux-actions'
import update from 'immutability-helper'
import {GET_ALL_CRITERIA_SUCCESS} from '../actions/review-actions'

const defaultState = {
  review: {
    criteria: []
  }
}

const admin = handleActions({
  [GET_ALL_CRITERIA_SUCCESS]: (state, action) => {
    return update(state, {
      review: { criteria: { $set: action.payload } }
    })
  }
}, defaultState)

export default admin
