// import { createAction } from 'redux-actions'
import { CALL_API } from 'redux-api-middleware'
import {
  getJsonHeaders,
  buildAPIRequest
} from './common-actions'
import { apiUrl } from '../config'

export const GET_ALL_CRITERIA_REQUEST = 'GET_ALL_CRITERIA_REQUEST'
export const GET_ALL_CRITERIA_SUCCESS = 'GET_ALL_CRITERIA_SUCCESS'
export const GET_ALL_CRITERIA_FAILURE = 'GET_ALL_CRITERIA_FAILURE'

export function fetchAllCriteria () {
  const endpoint = `${apiUrl}/review`
  const apiTypes = [
    GET_ALL_CRITERIA_REQUEST,
    GET_ALL_CRITERIA_SUCCESS,
    GET_ALL_CRITERIA_FAILURE]
  return {
    [CALL_API]: buildAPIRequest(endpoint, 'GET', getJsonHeaders(), apiTypes)
  }
}

export const ADD_CRITERION_REQUEST = 'ADD_CRITERION_REQUEST'
export const ADD_CRITERION_SUCCESS = 'ADD_CRITERION_SUCCESS'
export const ADD_CRITERION_FAILURE = 'ADD_CRITERION_FAILURE'

export function addNewCriterion (criterion) {
  const endpoint = `${apiUrl}/review/criteria`
  const apiTypes = [
    ADD_CRITERION_REQUEST,
    ADD_CRITERION_SUCCESS,
    ADD_CRITERION_FAILURE]
  const body = {
    ...criterion,
    editable: criterion.isEditable
  }
  return {
    [CALL_API]: buildAPIRequest(endpoint, 'POST', getJsonHeaders(), apiTypes,
      JSON.stringify(body))
  }
}

export const DELETE_CRITERION_REQUEST = 'DELETE_CRITERION_REQUEST'
export const DELETE_CRITERION_SUCCESS = 'DELETE_CRITERION_SUCCESS'
export const DELETE_CRITERION_FAILURE = 'DELETE_CRITERION_FAILURE'
export function removeCriterion (id) {
  const endpoint = `${apiUrl}/review/criteria/${id}`
  const types = [DELETE_CRITERION_REQUEST, DELETE_CRITERION_SUCCESS,
    DELETE_CRITERION_FAILURE]
  return {
    [CALL_API]: buildAPIRequest(endpoint, 'DELETE', getJsonHeaders(), types)
  }
}
